package com.tournament.tournament_manager.config.security;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsServiceImpl userDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private FilterChain filterChain;

    private MockHttpServletResponse response;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        response = new MockHttpServletResponse();
        ObjectMapper objectMapper = JsonMapper.builder().build();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userDetailsService, objectMapper);
    }

    @Test
    void doFilterInternal_shouldPassThrough_whenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_shouldPassThrough_whenAuthHeaderNotBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_shouldAuthenticateUser_whenValidToken() throws Exception {
        User userDetails = new User("admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", "admin")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void doFilterInternal_shouldNotAuthenticate_whenTokenInvalid() throws Exception {
        User userDetails = new User("admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.extractUsername("invalid-token")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid-token", "admin")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldPassThrough_whenUsernameIsNull() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
        when(jwtService.extractUsername("some-token")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    /**
     * Régression du point 1.3 de la revue : un token expiré ne doit plus court-circuiter la
     * chaîne. Sans cela, un client au token périmé qui appelle {@code POST /api/auth/login}
     * (endpoint public) sans avoir retiré son ancien header {@code Authorization} reçoit un
     * 401 avant d'atteindre le contrôleur — impossible de se ré-authentifier.
     */
    @Test
    void doFilterInternal_shouldContinueChain_whenTokenExpired() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(jwtService.extractUsername("expired-token"))
                .thenThrow(new io.jsonwebtoken.ExpiredJwtException(
                        (io.jsonwebtoken.Header) null, (io.jsonwebtoken.Claims) null, "expiré"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_shouldContinueChain_whenTokenMalformed() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer not-a-jwt");
        when(jwtService.extractUsername("not-a-jwt")).thenThrow(new MalformedJwtException("illisible"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilterInternal_shouldContinueChain_whenUserNoLongerExists() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("User not found: ghost"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /**
     * Régression du point 1.3 de la revue : une panne de base ne doit plus être maquillée en
     * « token invalide ». Elle produisait un 401, ce qui faussait la supervision (pic de 401
     * pendant un incident Postgres) et envoyait le client sur une fausse piste.
     */
    @Test
    void doFilterInternal_shouldReturn503_whenTechnicalFailure() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin"))
                .thenThrow(new DataAccessResourceFailureException("pool épuisé"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
    }
}
