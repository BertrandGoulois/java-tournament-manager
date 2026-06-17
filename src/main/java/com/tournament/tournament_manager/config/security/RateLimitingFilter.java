package com.tournament.tournament_manager.config.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Filtre de rate limiting par IP sur les endpoints sensibles.
 *
 * <p>Limites appliquées :
 * <ul>
 *   <li>{@code POST /api/auth/login} — 5 tentatives par minute</li>
 *   <li>{@code POST /api/players} — 10 créations par minute</li>
 * </ul>
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> createPlayerBuckets = new ConcurrentHashMap<>();

    @Value("${rate-limiting.login.capacity:5}")
    private int loginCapacity = 5;

    @Value("${rate-limiting.player.capacity:10}")
    private int playerCapacity = 10;

    @Value("${rate-limiting.trusted-proxies:}")
    private String trustedProxiesRaw = "";

    private Set<String> trustedProxies = new HashSet<>();

    private Bucket newLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(loginCapacity)
                        .refillIntervally(loginCapacity, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket newCreatePlayerBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(playerCapacity)
                        .refillIntervally(playerCapacity, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    @PostConstruct
    public void init() {
        trustedProxies = Arrays.stream(trustedProxiesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            Bucket bucket = loginBuckets.computeIfAbsent(ip, k -> newLoginBucket());
            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Trop de tentatives de connexion. Réessayez dans 1 minute.");
                return;
            }
        }

        if ("POST".equals(method) && "/api/players".equals(path)) {
            Bucket bucket = createPlayerBuckets.computeIfAbsent(ip, k -> newCreatePlayerBucket());
            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("Trop de créations de joueurs. Réessayez dans 1 minute.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Détermine l'IP cliente à utiliser pour le rate limiting.
     *
     * <p>Le header {@code X-Forwarded-For} n'est pris en compte que si la requête
     * provient d'une source explicitement déclarée de confiance (reverse proxy connu),
     * configurée via {@code rate-limiting.trusted-proxies}. Sans cela, n'importe quel
     * client pourrait falsifier ce header pour contourner le rate limiting par IP.
     *
     * @param request la requête HTTP entrante
     * @return l'IP à utiliser pour le bucket de rate limiting
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