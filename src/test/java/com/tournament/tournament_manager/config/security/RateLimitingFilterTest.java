package com.tournament.tournament_manager.config.security;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
class RateLimitingFilterTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    private RateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() throws Exception {
        RedisClient redisClient = RedisClient.create(
                "redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));

        var connection = redisClient.connect(
                RedisCodec.of(
                        StringCodec.UTF8,
                        ByteArrayCodec.INSTANCE));

        ProxyManager<String> proxyManager =
                LettuceBasedProxyManager.builderFor(connection)
                        .withExpirationStrategy(
                                ExpirationAfterWriteStrategy
                                        .basedOnTimeForRefillingBucketUpToMax(java.time.Duration.ofMinutes(2)))
                        .build();

        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        filter = new RateLimitingFilter(proxyManager, meterRegistry);
        filter.init();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void login_shouldAllow_withinLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = buildRequest("POST", "/api/auth/login", "1.2.3.4");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    void login_shouldBlock_whenLimitExceeded() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(
                    buildRequest("POST", "/api/auth/login", "5.5.5.5"),
                    new MockHttpServletResponse(),
                    filterChain);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                buildRequest("POST", "/api/auth/login", "5.5.5.5"),
                response,
                filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Trop de tentatives");
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    void login_shouldIsolate_perIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(
                    buildRequest("POST", "/api/auth/login", "10.0.0.1"),
                    new MockHttpServletResponse(),
                    filterChain);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                buildRequest("POST", "/api/auth/login", "10.0.0.2"),
                response,
                filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void createPlayer_shouldAllow_withinLimit() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = buildRequest("POST", "/api/players", "3.3.3.3");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(filterChain, times(10)).doFilter(any(), any());
    }

    @Test
    void createPlayer_shouldBlock_whenLimitExceeded() throws Exception {
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(
                    buildRequest("POST", "/api/players", "4.4.4.4"),
                    new MockHttpServletResponse(),
                    filterChain);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                buildRequest("POST", "/api/players", "4.4.4.4"),
                response,
                filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Trop de créations");
    }

    @Test
    void otherRoute_shouldNotBeRateLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest request = buildRequest("GET", "/api/players", "6.6.6.6");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private MockHttpServletRequest buildRequest(String method, String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        if (remoteAddr != null) request.setRemoteAddr(remoteAddr);
        return request;
    }
}