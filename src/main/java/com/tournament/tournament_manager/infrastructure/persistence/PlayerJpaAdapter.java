package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.out.*;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import com.tournament.tournament_manager.repository.EloHistoryRepository;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.repository.PlayerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant les ports de chargement, sauvegarde
 * et consultation des joueurs.
 */
@Component
public class PlayerJpaAdapter implements LoadPlayerPort, SavePlayerPort,
        ExistsPlayerPort, LoadAllPlayersPort, CountMatchesByPlayerPort, LoadEloHistoryPort {

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final EloHistoryRepository eloHistoryRepository;

    public PlayerJpaAdapter(PlayerRepository playerRepository,
                            MatchRepository matchRepository,
                            EloHistoryRepository eloHistoryRepository) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.eloHistoryRepository = eloHistoryRepository;
    }

    @Override
    public Player loadPlayer(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
    }

    @Override
    public Player savePlayer(Player player) {
        return playerRepository.save(player);
    }

    @Override
    public boolean existsByUsername(String username) {
        return playerRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return playerRepository.existsByEmail(email);
    }

    @Override
    public Page<Player> loadAllPlayers(Pageable pageable) {
        return playerRepository.findAll(pageable);
    }

    @Override
    public long countByPlayer(Long playerId) {
        return matchRepository.countByPlayer1IdOrPlayer2Id(playerId, playerId);
    }

    @Override
    public long countWinsByPlayer(Long playerId) {
        return matchRepository.countByWinnerId(playerId);
    }

    @Override
    public List<EloHistory> loadByPlayerIdOrderByDateDesc(Long playerId) {
        return eloHistoryRepository.findByPlayerIdOrderByCreatedAtDesc(playerId);
    }
}