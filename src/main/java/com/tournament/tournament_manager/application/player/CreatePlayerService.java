package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.CreatePlayerCommand;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.port.in.player.CreatePlayerUseCase;
import com.tournament.tournament_manager.domain.port.out.player.ExistsPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.SavePlayerPort;
import com.tournament.tournament_manager.exception.domain.PlayerAlreadyExistsException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : création d'un joueur. Retourne un objet de domaine pur — voir la
 * Javadoc de {@code GetPlayerService}.
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
    public Player createPlayer(CreatePlayerCommand command) {
        if (existsPlayerPort.existsByUsername(command.username())) {
            log.warn("Tentative de création avec un username déjà utilisé [username='{}']", command.username());
            throw new PlayerAlreadyExistsException("username", command.username());
        }
        if (existsPlayerPort.existsByEmail(command.email())) {
            log.warn("Tentative de création avec un email déjà utilisé [email='{}']", command.email());
            throw new PlayerAlreadyExistsException("email", command.email());
        }
        Player player = new Player();
        player.setUsername(command.username());
        player.setEmail(command.email());
        playerCreatedCounter.increment();
        Player saved = savePlayerPort.savePlayer(player);
        log.info("Joueur créé [id={}, username='{}']", saved.getId(), saved.getUsername());
        return saved;
    }
}
