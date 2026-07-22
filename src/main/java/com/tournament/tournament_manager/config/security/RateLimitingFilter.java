package com.tournament.tournament_manager.config.security;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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
 *   <li>{@code POST /api/players} — 10 créations par minute (fenêtre glissante)</li>
 * </ul>
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;

    private Counter rateLimitBlockedCounter;

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

        String ip = getClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            var bucket = proxyManager.builder().build("rate-limit:login:" + ip, loginBucketConfig());
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit dépassé sur /api/auth/login [ip={}]", ip);
                rateLimitBlockedCounter.increment();
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Trop de tentatives de connexion. Réessayez dans 1 minute.");
                return;
            }
        }

        if ("POST".equals(method) && "/api/players".equals(path)) {
            var bucket = proxyManager.builder().build("rate-limit:player:" + ip, createPlayerBucketConfig());
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit dépassé sur /api/players [ip={}]", ip);
                rateLimitBlockedCounter.increment();
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Trop de créations de joueurs. Réessayez dans 1 minute.");
                return;
            }
        }

        filterChain.doFilter(request, response);
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
}