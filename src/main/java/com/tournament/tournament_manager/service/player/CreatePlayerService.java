package com.tournament.tournament_manager.service.player;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.in.player.CreatePlayerUseCase;
import com.tournament.tournament_manager.domain.port.out.player.ExistsPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.SavePlayerPort;
import com.tournament.tournament_manager.dto.request.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.PlayerResponse;
import com.tournament.tournament_manager.exception.PlayerAlreadyExistsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : création d'un joueur.
 */
@Service
@Transactional
public class CreatePlayerService implements CreatePlayerUseCase {

    private final SavePlayerPort savePlayerPort;
    private final ExistsPlayerPort existsPlayerPort;

    public CreatePlayerService(SavePlayerPort savePlayerPort,
                               ExistsPlayerPort existsPlayerPort) {
        this.savePlayerPort = savePlayerPort;
        this.existsPlayerPort = existsPlayerPort;
    }

    @Override
    public PlayerResponse createPlayer(CreatePlayerRequest request) {
        if (existsPlayerPort.existsByUsername(request.username())) {
            throw new PlayerAlreadyExistsException("username", request.username());
        }
        if (existsPlayerPort.existsByEmail(request.email())) {
            throw new PlayerAlreadyExistsException("email", request.email());
        }
        Player player = new Player();
        player.setUsername(request.username());
        player.setEmail(request.email());
        return toResponse(savePlayerPort.savePlayer(player));
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