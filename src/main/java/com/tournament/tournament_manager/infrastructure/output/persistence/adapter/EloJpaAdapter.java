package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.out.elo.ExistsEloHistoryPort;
import com.tournament.tournament_manager.domain.port.out.elo.SaveAllPlayersPort;
import com.tournament.tournament_manager.domain.port.out.elo.SaveEloHistoryPort;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.EloHistoryRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant les ports de sauvegarde ELO et historique.
 */
@Component
public class EloJpaAdapter implements SaveAllPlayersPort, SaveEloHistoryPort, ExistsEloHistoryPort {

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

    @Override
    public boolean existsByMatchId(Long matchId) {
        return eloHistoryRepository.existsByMatchId(matchId);
    }
}