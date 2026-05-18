package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.out.SaveAllPlayersPort;
import com.tournament.tournament_manager.domain.port.out.SaveEloHistoryPort;
import com.tournament.tournament_manager.repository.EloHistoryRepository;
import com.tournament.tournament_manager.repository.PlayerRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant les ports de sauvegarde ELO et historique.
 */
@Component
public class EloJpaAdapter implements SaveAllPlayersPort, SaveEloHistoryPort {

    private final PlayerRepository playerRepository;
    private final EloHistoryRepository eloHistoryRepository;

    public EloJpaAdapter(PlayerRepository playerRepository,
                         EloHistoryRepository eloHistoryRepository) {
        this.playerRepository = playerRepository;
        this.eloHistoryRepository = eloHistoryRepository;
    }

    @Override
    public void saveAllPlayers(List<Player> players) {
        playerRepository.saveAll(players);
    }

    @Override
    public void saveEloHistory(EloHistory eloHistory) {
        eloHistoryRepository.save(eloHistory);
    }
}