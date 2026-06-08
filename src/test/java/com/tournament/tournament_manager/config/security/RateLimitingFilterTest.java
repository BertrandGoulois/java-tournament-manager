package com.tournament.tournament_manager.config.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
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
                    buildRequest("POST", "/api/auth/login", "1.2.3.4"),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                buildRequest("POST", "/api/auth/login", "1.2.3.4"),
                response,
                filterChain
        );
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Trop de tentatives");
        verify(filterChain, times(5)).doFilter(any(), any()); // la 6e ne passe pas
    }

    @Test
    void login_shouldIsolate_perIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(
                    buildRequest("POST", "/api/auth/login", "1.1.1.1"),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                buildRequest("POST", "/api/auth/login", "2.2.2.2"),
                response,
                filterChain
        );
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void createPlayer_shouldAllow_withinLimit() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = buildRequest("POST", "/api/players", "1.2.3.4");
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
                    buildRequest("POST", "/api/players", "1.2.3.4"),
                    new MockHttpServletResponse(),
                    filterChain
            );
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(
                buildRequest("POST", "/api/players", "1.2.3.4"),
                response,
                filterChain
        );
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("Trop de créations");
    }

    @Test
    void otherRoute_shouldNotBeRateLimited() throws Exception {
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest request = buildRequest("GET", "/api/players", "1.2.3.4");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, filterChain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void xForwardedFor_shouldBeUsedAsIp() throws Exception {
        MockHttpServletRequest request = buildRequest("POST", "/api/auth/login", null);
        request.addHeader("X-Forwarded-For", "9.9.9.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest buildRequest(String method, String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        if (remoteAddr != null) request.setRemoteAddr(remoteAddr);
        return request;
    }
}