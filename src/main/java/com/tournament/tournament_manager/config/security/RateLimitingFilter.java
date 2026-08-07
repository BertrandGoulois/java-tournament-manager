package com.tournament.tournament_manager.config.security;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.bucket4j.Bandwidth.builder;

/**
 * Filtre de rate limiting par IP sur les endpoints sensibles, avec état distribué via Redis.
 *
 * <p>Les buckets sont stockés dans Redis via {@link ProxyManager}, ce qui garantit
 * que le rate limiting fonctionne correctement même sur plusieurs instances de l'application.
 *
 * <p>Limites appliquées :
 * <ul>
 *   <li>{@code POST /api/auth/login} — 5 tentatives par minute (fenêtre glissante)</li>
 *   <li>{@code POST /api/players}, ainsi que {@code POST /api/rpc} avec
 *       {@code method: "player.create"} (même opération, exposée par une seconde porte
 *       d'entrée) — 10 créations par minute (fenêtre glissante)</li>
 * </ul>
 *
 * <p>Mode dégradé : si Redis est indisponible, la requête est laissée passer plutôt que
 * rejetée en 500 — le rate limiting est une protection en profondeur, pas une garantie de
 * disponibilité ; la faire dépendre d'une dépendance externe reviendrait à transformer une
 * panne Redis en panne totale de ces endpoints. Chaque échec est journalisé et compté.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;

    private Counter rateLimitBlockedCounter;
    private Counter rateLimitDegradedCounter;

    private static final JsonMapper RPC_BODY_MAPPER = JsonMapper.builder().build();
    private static final String PLAYER_CREATE_RPC_METHOD = "player.create";

    @Value("${rate-limiting.login.capacity:5}")
    private int loginCapacity = 5;

    @Value("${rate-limiting.player.capacity:10}")
    private int playerCapacity = 10;

    @Value("${rate-limiting.trusted-proxies:}")
    private String trustedProxiesRaw = "";

    private Set<String> trustedProxies = new HashSet<>();

    public RateLimitingFilter(ProxyManager<String> proxyManager, MeterRegistry meterRegistry) {
        this.proxyManager = proxyManager;
        this.rateLimitBlockedCounter = Counter.builder("rate.limit.blocked")
                .description("Nombre de requêtes bloquées par le rate limiting")
                .register(meterRegistry);
        this.rateLimitDegradedCounter = Counter.builder("rate.limit.degraded")
                .description("Nombre de requêtes laissées passer sans vérification, Redis étant indisponible")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        trustedProxies = Arrays.stream(trustedProxiesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

    }

    private Supplier<BucketConfiguration> loginBucketConfig() {
        return () -> BucketConfiguration.builder()
                .addLimit(builder()
                        .capacity(loginCapacity)
                        .refillGreedy(loginCapacity, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Supplier<BucketConfiguration> createPlayerBucketConfig() {
        return () -> BucketConfiguration.builder()
                .addLimit(builder()
                        .capacity(playerCapacity)
                        .refillGreedy(playerCapacity, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && "/api/rpc".equals(path)) {
            doFilterRpc(request, response, filterChain);
            return;
        }

        String ip = getClientIp(request);

        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            if (isRateLimited(ip, "rate-limit:login:" + ip, loginBucketConfig())) {
                rejectTooManyRequests(response, "Trop de tentatives de connexion. Réessayez dans 1 minute.",
                        "/api/auth/login", ip);
                return;
            }
        }

        if ("POST".equals(method) && "/api/players".equals(path)) {
            if (isRateLimited(ip, "rate-limit:player:" + ip, createPlayerBucketConfig())) {
                rejectTooManyRequests(response, "Trop de créations de joueurs. Réessayez dans 1 minute.",
                        "/api/players", ip);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Cas particulier de {@code POST /api/rpc} : la même opération de création de joueur
     * ({@code player.create}) y est accessible sous un nom de méthode JSON-RPC plutôt qu'un
     * chemin REST dédié. Il faut lire le corps pour le savoir — on le met en cache dans
     * {@link ReplayableBodyRequestWrapper} pour qu'il reste intégralement relisible en aval
     * (désérialisation JSON-RPC normale par le contrôleur). Contrairement à
     * {@code ContentCachingRequestWrapper}, ce wrapper ne consomme pas le flux original :
     * son {@code getInputStream()} rejoue toujours les octets mis en cache.
     */
    private void doFilterRpc(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        byte[] body = request.getInputStream().readAllBytes();
        ReplayableBodyRequestWrapper replayableRequest = new ReplayableBodyRequestWrapper(request, body);

        String rpcMethod = extractRpcMethod(body);
        if (PLAYER_CREATE_RPC_METHOD.equals(rpcMethod)) {
            String ip = getClientIp(request);
            if (isRateLimited(ip, "rate-limit:player:" + ip, createPlayerBucketConfig())) {
                rejectTooManyRequests(response, "Trop de créations de joueurs. Réessayez dans 1 minute.",
                        "/api/rpc (player.create)", ip);
                return;
            }
        }

        filterChain.doFilter(replayableRequest, response);
    }

    private String extractRpcMethod(byte[] body) {
        if (body.length == 0) {
            return null;
        }
        try {
            JsonNode root = RPC_BODY_MAPPER.readTree(new String(body, StandardCharsets.UTF_8));
            JsonNode methodNode = root.get("method");
            return methodNode != null ? methodNode.asString(null) : null;
        } catch (Exception e) {
            // Corps JSON-RPC malformé : ce n'est pas au filtre de rate limiting de le rejeter,
            // le contrôleur s'en chargera avec un message d'erreur approprié.
            return null;
        }
    }

    /**
     * Consomme un jeton du bucket identifié par {@code bucketKey}. Renvoie {@code true} si la
     * limite est dépassée (requête à rejeter), {@code false} sinon — y compris si Redis est
     * indisponible (mode dégradé : on ne bloque pas le trafic pour une panne d'une dépendance
     * annexe à la protection elle-même).
     */
    private boolean isRateLimited(String ip, String bucketKey, Supplier<BucketConfiguration> bucketConfig) {
        try {
            Bucket bucket = proxyManager.builder().build(bucketKey, bucketConfig);
            return !bucket.tryConsume(1);
        } catch (Exception e) {
            log.warn("Rate limiting indisponible (Redis injoignable ?), requête laissée passer sans "
                    + "vérification [ip={}, bucket={}] : {}", ip, bucketKey, e.getMessage());
            rateLimitDegradedCounter.increment();
            return false;
        }
    }

    private void rejectTooManyRequests(HttpServletResponse response, String message, String endpoint, String ip)
            throws IOException {
        log.warn("Rate limit dépassé sur {} [ip={}]", endpoint, ip);
        rateLimitBlockedCounter.increment();
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.getWriter().write(message);
    }

    /**
     * Détermine l'IP cliente à utiliser pour le rate limiting.
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean trustedSource = trustedProxies.contains("*") || trustedProxies.contains(remoteAddr);

        if (trustedSource) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    /**
     * Wrapper de requête dont {@link #getInputStream()} rejoue toujours les octets fournis à la
     * construction, plutôt que de consommer un flux sous-jacent une seule fois. Nécessaire ici
     * car {@code ContentCachingRequestWrapper} de Spring, malgré son nom, ne permet pas de
     * relire le corps via {@code getInputStream()} après l'avoir consommé une première fois —
     * seul {@code getContentAsByteArray()} reste utilisable, ce qui cassait la désérialisation
     * JSON-RPC faite plus loin dans la chaîne par le contrôleur.
     */
    private static final class ReplayableBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        ReplayableBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Corps déjà entièrement disponible en mémoire : lecture toujours synchrone,
                    // aucune notification asynchrone à déclencher.
                }

                @Override
                public int read() {
                    return byteStream.read();
                }
            };
        }
    }
}