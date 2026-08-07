package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.RefreshToken;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RefreshTokenEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.RefreshTokenMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenJpaAdapterTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenJpaAdapter refreshTokenJpaAdapter;

    @BeforeEach
    void setUp() {
        refreshTokenJpaAdapter = new RefreshTokenJpaAdapter(refreshTokenRepository, new RefreshTokenMapper());
    }

    @Test
    void saveRefreshToken_shouldCreateNewEntity_whenNoId() {
        RefreshToken token = new RefreshToken();
        token.setToken("hash123");
        token.setUsername("admin");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> {
            RefreshTokenEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        RefreshToken result = refreshTokenJpaAdapter.saveRefreshToken(token);

        assertNotNull(result);
        assertNotNull(result.getId());
    }

    @Test
    void loadByToken_shouldReturnToken_whenFound() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(1L);
        entity.setToken("abc");
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(entity));

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
