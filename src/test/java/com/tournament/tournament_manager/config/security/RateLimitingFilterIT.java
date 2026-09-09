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
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
class RateLimitingFilterIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
            .withExposedPorts(6379);

    private RateLimitingFilter filter;
    private FilterChain filterChain;
    private ProxyManager<String> proxyManager;

    @BeforeEach
    void setUp() throws Exception {
        RedisClient redisClient = RedisClient.create(
                "redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));

        var connection = redisClient.connect(
                RedisCodec.of(
                        StringCodec.UTF8,
                        ByteArrayCodec.INSTANCE));

        proxyManager =
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

    @Test
    void rpcPlayerCreate_shouldAllow_withinLimit() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = buildRpcRequest("player.create", "7.7.7.7");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
        verify(filterChain, times(10)).doFilter(any(), any());
    }

    @Test
    void rpcPlayerCreate_shouldBlock_whenLimitExceeded() throws Exception {
        // Même bucket que POST /api/players : c'est la même opération, exposée par une
        // seconde porte d'entrée (JSON-RPC) — le contournement que corrige ce point.
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(
                    buildRpcRequest("player.create", "8.8.8.8"),
                    new MockHttpServletResponse(),
                    filterChain);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                buildRpcRequest("player.create", "8.8.8.8"),
                response,
                filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Trop de créations");
    }

    @Test
    void rpcPlayerCreate_shouldShareBucket_withRestEndpoint() throws Exception {
        // La même IP consomme le même bucket qu'elle passe par REST ou par JSON-RPC.
        String ip = "9.9.9.9";
        for (int i = 0; i < 6; i++) {
            filter.doFilterInternal(
                    buildRequest("POST", "/api/players", ip),
                    new MockHttpServletResponse(),
                    filterChain);
        }
        for (int i = 0; i < 4; i++) {
            filter.doFilterInternal(
                    buildRpcRequest("player.create", ip),
                    new MockHttpServletResponse(),
                    filterChain);
        }
        // Le 11e appel, quelle que soit la porte d'entrée, doit être bloqué.
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(buildRpcRequest("player.create", ip), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void rpcOtherMethod_shouldNotBeRateLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest request = buildRpcRequest("tournament.getAll", "11.11.11.11");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void rpcRequest_shouldRemainReadable_byControllerDownstream() throws Exception {
        // Le filtre doit lire le corps pour repérer "method", sans empêcher le contrôleur
        // de le désérialiser normalement plus loin dans la chaîne.
        MockHttpServletRequest request = buildRpcRequest("tournament.getAll", "12.12.12.12");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(argThat(req -> {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return body.contains("tournament.getAll");
            } catch (Exception e) {
                return false;
            }
        }), any());
    }

    @Test
    void shouldFailOpen_whenRedisUnavailable() throws Exception {
        @SuppressWarnings("unchecked")
        ProxyManager<String> brokenProxyManager = mock(ProxyManager.class);
        when(brokenProxyManager.builder()).thenThrow(new RuntimeException("Redis injoignable"));

        RateLimitingFilter degradedFilter =
                new RateLimitingFilter(brokenProxyManager, new SimpleMeterRegistry());
        degradedFilter.init();

        MockHttpServletResponse response = new MockHttpServletResponse();
        degradedFilter.doFilterInternal(
                buildRequest("POST", "/api/auth/login", "13.13.13.13"),
                response,
                filterChain);

        // Redis est en panne : on laisse passer plutôt que de renvoyer une 500 sur
        // un endpoint qui n'a par ailleurs aucun rapport avec Redis.
        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    // ------------------------------------------------------------------------------
    // Point 1.2 de la revue — identification du client derrière un reverse proxy.
    //
    // C'est le bug le plus grave du projet, et aucun des tests ci-dessus ne pouvait
    // l'attraper : ils valident tous le composant avec un remoteAddr direct, jamais la
    // topologie de déploiement réelle (nginx devant l'application). Or c'est exactement là
    // que le filtre était inopérant — trusted-proxies valait "172.16.0.0/12", comparé par
    // égalité de chaînes à une IP comme "172.18.0.7", donc jamais fiable, donc
    // X-Forwarded-For jamais lu, donc TOUT le trafic dans un seul bucket à l'IP de nginx.
    // ------------------------------------------------------------------------------

    /** Construit un filtre avec une liste de proxys de confiance donnée. */
    private RateLimitingFilter filterTrusting(String trustedProxies) {
        RateLimitingFilter f = new RateLimitingFilter(proxyManager, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(f, "trustedProxiesRaw", trustedProxies);
        f.init();
        return f;
    }

    private MockHttpServletRequest buildProxiedRequest(String uri, String proxyAddr, String forwardedFor) {
        MockHttpServletRequest request = buildRequest("POST", uri, proxyAddr);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }

    /**
     * Le test qui aurait attrapé le bug. Deux clients distincts derrière le même nginx
     * doivent avoir deux buckets distincts. Avant le correctif, le 6e appel — pourtant le
     * premier d'un autre client — recevait un 429.
     */
    @Test
    void login_shouldIsolatePerClient_whenBehindTrustedProxyDeclaredAsCidr() throws Exception {
        RateLimitingFilter proxied = filterTrusting("172.16.0.0/12");
        String nginx = "172.18.0.7";

        for (int i = 0; i < 5; i++) {
            proxied.doFilterInternal(
                    buildProxiedRequest("/api/auth/login", nginx, "203.0.113.10"),
                    new MockHttpServletResponse(),
                    filterChain);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        proxied.doFilterInternal(
                buildProxiedRequest("/api/auth/login", nginx, "203.0.113.10"),
                blocked, filterChain);
        assertThat(blocked.getStatus()).isEqualTo(429);

        // Autre client, même nginx : bucket vierge.
        MockHttpServletResponse other = new MockHttpServletResponse();
        proxied.doFilterInternal(
                buildProxiedRequest("/api/auth/login", nginx, "203.0.113.11"),
                other, filterChain);
        assertThat(other.getStatus()).isEqualTo(200);
    }

    /**
     * Symétrique du précédent : X-Forwarded-For posé par un pair non déclaré de confiance
     * doit être ignoré, sinon n'importe qui obtient un bucket neuf à volonté.
     */
    @Test
    void login_shouldIgnoreForwardedFor_whenPeerIsNotATrustedProxy() throws Exception {
        RateLimitingFilter proxied = filterTrusting("172.16.0.0/12");
        String attacker = "198.51.100.4";

        for (int i = 0; i < 5; i++) {
            proxied.doFilterInternal(
                    buildProxiedRequest("/api/auth/login", attacker, "1.1.1." + i),
                    new MockHttpServletResponse(),
                    filterChain);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        proxied.doFilterInternal(
                buildProxiedRequest("/api/auth/login", attacker, "1.1.1.99"),
                response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    /**
     * Un client qui pose lui-même un X-Forwarded-For voit nginx AJOUTER son IP réelle à
     * droite (proxy_add_x_forwarded_for). La lecture de droite à gauche retient donc
     * l'adresse réellement prouvée. L'ancien {@code split(",")[0]} retenait au contraire la
     * valeur inventée par le client — un bucket neuf à chaque requête.
     */
    @Test
    void login_shouldUseRightmostUntrustedHop_whenClientSpoofsForwardedFor() throws Exception {
        RateLimitingFilter proxied = filterTrusting("172.16.0.0/12");
        String nginx = "172.18.0.7";

        for (int i = 0; i < 5; i++) {
            proxied.doFilterInternal(
                    buildProxiedRequest("/api/auth/login", nginx, "9.9.9." + i + ", 203.0.113.20"),
                    new MockHttpServletResponse(),
                    filterChain);
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        proxied.doFilterInternal(
                buildProxiedRequest("/api/auth/login", nginx, "9.9.9.42, 203.0.113.20"),
                response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
    }

    /** Une IP exacte reste une valeur acceptable dans trusted-proxies, à côté des CIDR. */
    @Test
    void shouldStillSupportExactIpEntries() throws Exception {
        RateLimitingFilter proxied = filterTrusting("10.0.0.1, 192.168.1.0/24");

        MockHttpServletResponse response = new MockHttpServletResponse();
        proxied.doFilterInternal(
                buildProxiedRequest("/api/auth/login", "10.0.0.1", "203.0.113.30"),
                response, filterChain);
        assertThat(response.getStatus()).isEqualTo(200);

        for (int i = 0; i < 5; i++) {
            proxied.doFilterInternal(
                    buildProxiedRequest("/api/auth/login", "192.168.1.50", "203.0.113.30"),
                    new MockHttpServletResponse(),
                    filterChain);
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        proxied.doFilterInternal(
                buildProxiedRequest("/api/auth/login", "10.0.0.1", "203.0.113.30"),
                blocked, filterChain);
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    /** Une entrée illisible ne doit pas faire échouer le démarrage ni invalider les autres. */
    @Test
    void shouldIgnoreMalformedTrustedProxyEntries() throws Exception {
        RateLimitingFilter proxied = filterTrusting("pas-une-ip, 172.16.0.0/12");

        MockHttpServletResponse response = new MockHttpServletResponse();
        proxied.doFilterInternal(
                buildProxiedRequest("/api/auth/login", "172.18.0.7", "203.0.113.40"),
                response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    /** Point 1.7 : le corps JSON-RPC n'est plus lu sans borne. */
    @Test
    void rpc_shouldReject_whenBodyExceedsLimit() throws Exception {
        RateLimitingFilter bounded = new RateLimitingFilter(proxyManager, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(bounded, "rpcMaxBodyBytes", 128);
        bounded.init();

        MockHttpServletRequest request = buildRequest("POST", "/api/rpc", "14.14.14.14");
        request.setContent(("{\"jsonrpc\":\"2.0\",\"method\":\"player.create\",\"params\":{\"x\":\""
                + "A".repeat(500) + "\"},\"id\":1}").getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        bounded.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(413);
        verify(filterChain, never()).doFilter(any(), any());
    }

    private MockHttpServletRequest buildRpcRequest(String rpcMethod, String remoteAddr) {
        MockHttpServletRequest request = buildRequest("POST", "/api/rpc", remoteAddr);
        String body = "{\"jsonrpc\":\"2.0\",\"method\":\"" + rpcMethod + "\",\"params\":{},\"id\":1}";
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setContentType("application/json");
        return request;
    }

    private MockHttpServletRequest buildRequest(String method, String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        if (remoteAddr != null) request.setRemoteAddr(remoteAddr);
        return request;
    }
}