package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.port.in.player.CreatePlayerUseCase;
import com.tournament.tournament_manager.domain.port.out.player.ExistsPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.SavePlayerPort;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.exception.domain.PlayerAlreadyExistsException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : création d'un joueur.
 */
@Slf4j
@Service
@Transactional
public class CreatePlayerService implements CreatePlayerUseCase {

    private final SavePlayerPort savePlayerPort;
    private final ExistsPlayerPort existsPlayerPort;
    private final Counter playerCreatedCounter;

    public CreatePlayerService(SavePlayerPort savePlayerPort,
                               ExistsPlayerPort existsPlayerPort,
                               MeterRegistry meterRegistry) {
        this.savePlayerPort = savePlayerPort;
        this.existsPlayerPort = existsPlayerPort;
        this.playerCreatedCounter = Counter.builder("player.created")
                .description("Nombre de joueurs créés")
                .register(meterRegistry);
    }

    @Override
    public PlayerResponse createPlayer(CreatePlayerRequest request) {
        if (existsPlayerPort.existsByUsername(request.username())) {
            log.warn("Tentative de création avec un username déjà utilisé [username='{}']", request.username());
            throw new PlayerAlreadyExistsException("username", request.username());
        }
        if (existsPlayerPort.existsByEmail(request.email())) {
            log.warn("Tentative de création avec un email déjà utilisé [email='{}']", request.email());
            throw new PlayerAlreadyExistsException("email", request.email());
        }
        Player player = new Player();
        player.setUsername(request.username());
        player.setEmail(request.email());
        playerCreatedCounter.increment();
        PlayerResponse response = toResponse(savePlayerPort.savePlayer(player));
        log.info("Joueur créé [id={}, username='{}']", response.id(), response.username());
        return response;
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