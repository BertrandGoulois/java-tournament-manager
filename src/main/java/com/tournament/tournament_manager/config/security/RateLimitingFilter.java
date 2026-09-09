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
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

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
 *
 * <p><b>Identification du client derrière un reverse proxy.</b> {@code rate-limiting.trusted-proxies}
 * accepte des IP exactes <i>et</i> des plages CIDR ({@code 172.16.0.0/12}), évaluées par
 * {@link IpAddressMatcher}. La version précédente comparait la propriété à
 * {@code request.getRemoteAddr()} par égalité de chaînes : une entrée CIDR ne pouvait donc
 * jamais correspondre, {@code X-Forwarded-For} n'était jamais lu en profil docker, et
 * <i>tout</i> le trafic entrant partageait un unique bucket à l'IP de nginx — soit 5 logins
 * par minute pour le monde entier, un déni de service à la portée de n'importe qui.
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

    /**
     * Taille maximale du corps JSON-RPC lu en mémoire par {@link #doFilterRpc}. En dessous
     * de cette borne le corps est mis en cache pour être relu par le contrôleur ; au-dessus,
     * la requête est rejetée en 413 sans jamais matérialiser le corps entier. Auparavant
     * {@code readAllBytes()} lisait sans limite : seul le {@code client_max_body_size} de
     * nginx (1 Mo par défaut, non explicite) faisait barrage, et uniquement derrière nginx.
     */
    @Value("${rate-limiting.rpc.max-body-bytes:1048576}")
    private int rpcMaxBodyBytes = 1_048_576;

    /** Renseigné par {@link #init()} : {@code true} si la liste contient le joker {@code *}. */
    private boolean trustAllProxies = false;

    /** Matchers CIDR/IP construits une fois au démarrage, jamais à chaque requête. */
    private List<IpAddressMatcher> trustedProxyMatchers = List.of();

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
        String raw = trustedProxiesRaw == null ? "" : trustedProxiesRaw;
        List<String> entries = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        trustAllProxies = entries.contains("*");

        List<IpAddressMatcher> matchers = new ArrayList<>();
        for (String entry : entries) {
            if ("*".equals(entry)) {
                continue;
            }
            try {
                matchers.add(new IpAddressMatcher(entry));
            } catch (IllegalArgumentException e) {
                // Une entrée illisible est ignorée plutôt que de faire échouer le démarrage,
                // mais elle est journalisée en ERROR : silencieusement ignorée, elle
                // reproduirait exactement le bug qu'on corrige ici (X-Forwarded-For jamais lu).
                log.error("Entrée invalide dans rate-limiting.trusted-proxies, ignorée [valeur='{}'] : {}",
                        entry, e.getMessage());
            }
        }
        trustedProxyMatchers = List.copyOf(matchers);

        if (trustAllProxies) {
            log.warn("rate-limiting.trusted-proxies contient '*' : X-Forwarded-For est accepté de "
                    + "n'importe quelle source, donc falsifiable. À réserver au développement.");
        } else {
            log.info("Rate limiting : {} proxy(s) de confiance configuré(s)", trustedProxyMatchers.size());
        }
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
     *
     * <p>La lecture est bornée à {@link #rpcMaxBodyBytes} : au-delà, la requête est rejetée
     * en 413 sans que le corps entier soit chargé en mémoire.
     */
    private void doFilterRpc(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        byte[] body = readBodyBounded(request);
        if (body == null) {
            log.warn("Corps JSON-RPC trop volumineux, requête rejetée [limite={} octets, ip={}]",
                    rpcMaxBodyBytes, getClientIp(request));
            response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
            response.getWriter().write("Corps de requête trop volumineux.");
            return;
        }
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

    /**
     * Lit le corps de la requête, en s'arrêtant dès que {@link #rpcMaxBodyBytes} est dépassé.
     *
     * @return les octets lus, ou {@code null} si la limite est franchie
     */
    private byte[] readBodyBounded(HttpServletRequest request) throws IOException {
        // On lit un octet de plus que la limite : si on l'obtient, c'est que le corps la dépasse.
        byte[] buffer = request.getInputStream().readNBytes(rpcMaxBodyBytes + 1);
        return buffer.length > rpcMaxBodyBytes ? null : buffer;
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
     *
     * <p>Si le pair TCP direct ({@code getRemoteAddr()}) n'est pas un proxy de confiance,
     * {@code X-Forwarded-For} est ignoré : n'importe qui peut poser cet en-tête.
     *
     * <p>Sinon, la chaîne {@code X-Forwarded-For} est parcourue <b>de droite à gauche</b> et
     * la première entrée non-fiable est retenue. C'est la seule lecture non falsifiable :
     * chaque proxy de confiance <i>ajoute</i> l'adresse de son propre pair à droite, donc
     * tout ce qu'un client peut écrire lui-même se retrouve à gauche de ce qu'il a réellement
     * prouvé. Prendre {@code split(",")[0]} (l'ancien comportement) revenait à faire confiance
     * à la partie de l'en-tête entièrement contrôlée par l'attaquant : il suffisait d'envoyer
     * {@code X-Forwarded-For: 1.2.3.4} avec une valeur différente à chaque requête pour obtenir
     * un bucket neuf à volonté et contourner intégralement la limitation.
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remoteAddr;
        }

        String[] hops = forwarded.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (!hop.isEmpty() && !isTrustedProxy(hop)) {
                return hop;
            }
        }
        // Toute la chaîne est interne (pas de client externe identifiable) : on retombe sur
        // le pair direct plutôt que de renvoyer une valeur arbitraire.
        return remoteAddr;
    }

    /**
     * {@code true} si {@code ip} appartient à l'une des plages de confiance configurées.
     * Une valeur non parsable (en-tête falsifié, nom d'hôte, IPv6 mal formée…) est traitée
     * comme non fiable — donc utilisable comme identifiant de bucket, jamais comme laissez-passer.
     */
    private boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        if (trustAllProxies) {
            return true;
        }
        for (IpAddressMatcher matcher : trustedProxyMatchers) {
            try {
                if (matcher.matches(ip)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // ip non parsable : ne peut correspondre à aucune plage, on passe à la suivante.
                return false;
            }
        }
        return false;
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
