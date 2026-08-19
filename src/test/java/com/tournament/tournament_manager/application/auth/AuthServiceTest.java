package com.tournament.tournament_manager.application.auth;

import com.tournament.tournament_manager.application.token.RefreshTokenService;
import com.tournament.tournament_manager.config.security.JwtService;
import com.tournament.tournament_manager.domain.model.AuthResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnToken_whenValidCredentials() {
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("admin", "password123")
        );
        when(jwtService.generateToken("admin")).thenReturn("jwt-token");
        when(refreshTokenService.generateRefreshToken("admin")).thenReturn("refresh-token");

        AuthResult result = authService.login("admin", "password123");

        assertEquals("jwt-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        verify(authenticationManager, times(1)).authenticate(any());
        verify(jwtService, times(1)).generateToken("admin");
        verify(refreshTokenService, times(1)).generateRefreshToken("admin");
    }

    @Test
    void login_shouldThrow_whenInvalidCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(
                new BadCredentialsException("Bad credentials")
        );

        assertThrows(BadCredentialsException.class,
                () -> authService.login("admin", "wrongpassword"));
    }
}