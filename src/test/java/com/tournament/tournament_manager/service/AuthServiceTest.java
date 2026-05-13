package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.config.security.JwtService;
import com.tournament.tournament_manager.dto.request.LoginRequest;
import com.tournament.tournament_manager.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnToken_whenValidCredentials() {
        LoginRequest request = new LoginRequest("admin", "password123");
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("admin", "password123")
        );
        when(jwtService.generateToken("admin")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.token());
        verify(authenticationManager, times(1)).authenticate(any());
        verify(jwtService, times(1)).generateToken("admin");
    }

    @Test
    void login_shouldThrow_whenInvalidCredentials() {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        when(authenticationManager.authenticate(any())).thenThrow(
                new org.springframework.security.authentication.BadCredentialsException("Bad credentials")
        );

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.login(request));
    }
}