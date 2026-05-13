package com.tournament.tournament_manager.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

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
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
        SecurityContextHolder.clearContext();

        User userDetails = new User("admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", "admin")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(jwtService, times(1)).isTokenValid("valid-token", "admin");
    }

    @Test
    void doFilterInternal_shouldNotAuthenticate_whenTokenInvalid() throws Exception {
        SecurityContextHolder.clearContext();

        User userDetails = new User("admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.extractUsername("invalid-token")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid-token", "admin")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldPassThrough_whenUsernameIsNull() throws Exception {
        SecurityContextHolder.clearContext();

        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
        when(jwtService.extractUsername("some-token")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }
}