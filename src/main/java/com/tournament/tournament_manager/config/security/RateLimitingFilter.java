package com.tournament.tournament_manager.config.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private int loginCapacity;

    @Value("${rate-limiting.player.capacity:10}")
    private int playerCapacity;

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

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}