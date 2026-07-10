package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerUseCase;
import com.tournament.tournament_manager.domain.port.out.player.LoadAllPlayersPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : consultation d'un ou plusieurs joueurs.
 */
@Service
@Transactional(readOnly = true)
public class GetPlayerService implements GetPlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final LoadAllPlayersPort loadAllPlayersPort;

    public GetPlayerService(LoadPlayerPort loadPlayerPort,
                            LoadAllPlayersPort loadAllPlayersPort) {
        this.loadPlayerPort = loadPlayerPort;
        this.loadAllPlayersPort = loadAllPlayersPort;
    }

    @Override
    public PlayerResponse getPlayerById(Long id) {
        return toResponse(loadPlayerPort.loadPlayer(id));
    }

    @Override
    public Page<PlayerResponse> getAllPlayers(Pageable pageable) {
        return loadAllPlayersPort.loadAllPlayers(pageable)
                .map(this::toResponse);
    }

    private PlayerResponse toResponse(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getUsername(),
                player.getEmail(),
                player.getEloRating().value(),
                player.getCreatedAt()
        );
    }
}