package com.tournament.tournament_manager.application.token;

import com.tournament.tournament_manager.config.security.JwtService;
import com.tournament.tournament_manager.domain.model.RefreshToken;
import com.tournament.tournament_manager.domain.port.out.auth.DeleteRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.LoadRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.SaveRefreshTokenPort;
import com.tournament.tournament_manager.domain.port.out.auth.UserExistsPort;
import com.tournament.tournament_manager.domain.model.AuthResult;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    @Mock
    private UserExistsPort userExistsPort;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    /** Reproduit le hash SHA-256 hex utilisé en interne par le service, pour construire des fixtures. */
    private static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void generateRefreshToken_shouldDeleteOldAndSaveNew() {
        when(saveRefreshTokenPort.saveRefreshToken(any())).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = refreshTokenService.generateRefreshToken("admin");

        verify(deleteRefreshTokenPort, times(1)).deleteByUsername("admin");
        verify(saveRefreshTokenPort, times(1)).saveRefreshToken(any());
        assertNotNull(rawToken);
    }

    @Test
    void generateRefreshToken_shouldPersistOnlyTheHash_neverTheRawToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(saveRefreshTokenPort.saveRefreshToken(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = refreshTokenService.generateRefreshToken("admin");

        RefreshToken persisted = captor.getValue();
        assertEquals("admin", persisted.getUsername());
        assertNotNull(persisted.getExpiryDate());
        // Le token persisté ne doit jamais être la valeur brute renvoyée au client...
        assertNotEquals(rawToken, persisted.getToken());
        // ...mais son hash SHA-256 hexadécimal.
        assertEquals(sha256Hex(rawToken), persisted.getToken());
    }

    @Test
    void refresh_shouldReturnNewAccessTokenAndRotateRefreshToken_whenTokenValid() {
        String rawToken = "raw-refresh-token";
        RefreshToken stored = new RefreshToken();
        stored.setToken(sha256Hex(rawToken));
        stored.setUsername("admin");
        stored.setRevoked(false);
        stored.setExpiryDate(Instant.now().plus(Duration.ofDays(1)));

        when(loadRefreshTokenPort.loadByToken(sha256Hex(rawToken))).thenReturn(Optional.of(stored));
        when(userExistsPort.existsByUsername("admin")).thenReturn(true);
        when(jwtService.generateToken("admin")).thenReturn("new-jwt");
        when(saveRefreshTokenPort.saveRefreshToken(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResult result = refreshTokenService.refresh(rawToken);

        assertEquals("new-jwt", result.accessToken());
        // Le refresh token retourné doit être NOUVEAU, jamais le même que celui présenté
        // (rotation) : un vol du token présenté ne sert plus à rien une fois utilisé.
        assertNotEquals(rawToken, result.refreshToken());
        assertNotNull(result.refreshToken());
        // Le token présenté doit avoir été marqué révoqué (une seule utilisation possible).
        assertTrue(stored.isRevoked());
    }

    @Test
    void refresh_shouldThrow_whenTokenNotFound() {
        when(loadRefreshTokenPort.loadByToken(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidException.class, () -> refreshTokenService.refresh("invalid"));
    }

    @Test
    void refresh_shouldThrow_whenTokenRevoked() {
        RefreshToken token = new RefreshToken();
        token.setUsername("admin");
        token.setRevoked(true);
        token.setExpiryDate(Instant.now().plus(Duration.ofDays(1)));

        when(loadRefreshTokenPort.loadByToken(anyString())).thenReturn(Optional.of(token));

        assertThrows(InvalidException.class, () -> refreshTokenService.refresh("refresh-token"));
        // Aucune rotation ne doit avoir lieu pour un token déjà révoqué.
        verify(saveRefreshTokenPort, never()).saveRefreshToken(any());
    }

    @Test
    void refresh_shouldThrow_whenTokenExpired() {
        RefreshToken token = new RefreshToken();
        token.setUsername("admin");
        token.setRevoked(false);
        token.setExpiryDate(Instant.now().minus(Duration.ofDays(1)));

        when(loadRefreshTokenPort.loadByToken(anyString())).thenReturn(Optional.of(token));

        assertThrows(InvalidException.class, () -> refreshTokenService.refresh("refresh-token"));
    }

    @Test
    void refresh_shouldThrow_whenUserNoLongerExists() {
        RefreshToken token = new RefreshToken();
        token.setUsername("deleted-user");
        token.setRevoked(false);
        token.setExpiryDate(Instant.now().plus(Duration.ofDays(1)));

        when(loadRefreshTokenPort.loadByToken(anyString())).thenReturn(Optional.of(token));
        when(userExistsPort.existsByUsername("deleted-user")).thenReturn(false);

        assertThrows(InvalidException.class, () -> refreshTokenService.refresh("refresh-token"));
        // Aucun nouvel access token ne doit être émis pour un compte qui n'existe plus.
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void revoke_shouldSetRevokedTrue() {
        RefreshToken token = new RefreshToken();
        token.setToken(sha256Hex("refresh-token"));
        token.setRevoked(false);

        when(loadRefreshTokenPort.loadByToken(sha256Hex("refresh-token"))).thenReturn(Optional.of(token));
        when(saveRefreshTokenPort.saveRefreshToken(any())).thenReturn(token);

        refreshTokenService.revoke("refresh-token");

        assertTrue(token.isRevoked());
        verify(saveRefreshTokenPort, times(1)).saveRefreshToken(token);
    }

    @Test
    void revoke_shouldThrow_whenTokenNotFound() {
        when(loadRefreshTokenPort.loadByToken(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidException.class, () -> refreshTokenService.revoke("invalid"));
    }
}
