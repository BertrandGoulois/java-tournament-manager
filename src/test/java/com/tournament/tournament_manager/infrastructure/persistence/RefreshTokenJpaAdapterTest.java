package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.RefreshToken;
import com.tournament.tournament_manager.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenJpaAdapterTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenJpaAdapter refreshTokenJpaAdapter;

    @Test
    void saveRefreshToken_shouldReturnSavedToken() {
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.save(any())).thenReturn(token);

        RefreshToken result = refreshTokenJpaAdapter.saveRefreshToken(token);

        assertNotNull(result);
    }

    @Test
    void loadByToken_shouldReturnToken_whenFound() {
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        Optional<RefreshToken> result = refreshTokenJpaAdapter.loadByToken("abc");

        assertTrue(result.isPresent());
    }

    @Test
    void loadByToken_shouldReturnEmpty_whenNotFound() {
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenJpaAdapter.loadByToken("abc");

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteByUsername_shouldCallRepository() {
        refreshTokenJpaAdapter.deleteByUsername("admin");

        verify(refreshTokenRepository, times(1)).deleteByUsername("admin");
    }
}