package com.tournament.tournament_manager.application.token;

import com.tournament.tournament_manager.config.security.JwtService;
import com.tournament.tournament_manager.domain.model.entities.RefreshToken;
import com.tournament.tournament_manager.domain.port.out.auth.DeleteRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.LoadRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.SaveRefreshTokenPort;
import com.tournament.tournament_manager.dto.response.auth.AuthResponse;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private SaveRefreshTokenPort saveRefreshTokenPort;
    @Mock
    private LoadRefreshTokenPort loadRefreshTokenPort;
    @Mock
    private DeleteRefreshTokenPort deleteRefreshTokenPort;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void generateRefreshToken_shouldDeleteOldAndSaveNew() {
        RefreshToken saved = new RefreshToken();
        saved.setToken("new-token");

        when(saveRefreshTokenPort.saveRefreshToken(any())).thenReturn(saved);

        String token = refreshTokenService.generateRefreshToken("admin");

        verify(deleteRefreshTokenPort, times(1)).deleteByUsername("admin");
        verify(saveRefreshTokenPort, times(1)).saveRefreshToken(any());
        assertEquals("new-token", token);
    }

    @Test
    void refresh_shouldReturnNewAccessToken_whenTokenValid() {
        RefreshToken token = new RefreshToken();
        token.setToken("refresh-token");
        token.setUsername("admin");
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(loadRefreshTokenPort.loadByToken("refresh-token")).thenReturn(Optional.of(token));
        when(jwtService.generateToken("admin")).thenReturn("new-jwt");

        AuthResponse response = refreshTokenService.refresh("refresh-token");

        assertEquals("new-jwt", response.token());
        assertEquals("refresh-token", response.refreshToken());
    }

    @Test
    void refresh_shouldThrow_whenTokenNotFound() {
        when(loadRefreshTokenPort.loadByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(InvalidException.class, () -> refreshTokenService.refresh("invalid"));
    }

    @Test
    void refresh_shouldThrow_whenTokenRevoked() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(true);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(loadRefreshTokenPort.loadByToken("refresh-token")).thenReturn(Optional.of(token));

        assertThrows(InvalidException.class, () -> refreshTokenService.refresh("refresh-token"));
    }

    @Test
    void refresh_shouldThrow_whenTokenExpired() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().minusDays(1));

        when(loadRefreshTokenPort.loadByToken("refresh-token")).thenReturn(Optional.of(token));

        assertThrows(InvalidException.class, () -> refreshTokenService.refresh("refresh-token"));
    }

    @Test
    void revoke_shouldSetRevokedTrue() {
        RefreshToken token = new RefreshToken();
        token.setToken("refresh-token");
        token.setRevoked(false);

        when(loadRefreshTokenPort.loadByToken("refresh-token")).thenReturn(Optional.of(token));
        when(saveRefreshTokenPort.saveRefreshToken(any())).thenReturn(token);

        refreshTokenService.revoke("refresh-token");

        assertTrue(token.isRevoked());
        verify(saveRefreshTokenPort, times(1)).saveRefreshToken(token);
    }

    @Test
    void revoke_shouldThrow_whenTokenNotFound() {
        when(loadRefreshTokenPort.loadByToken("invalid")).thenReturn(Optional.empty());

        assertThrows(InvalidException.class, () -> refreshTokenService.revoke("invalid"));
    }
}