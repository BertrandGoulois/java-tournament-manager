package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.EloHistory;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.port.out.elo.ExistsEloHistoryPort;
import com.tournament.tournament_manager.domain.port.out.elo.SaveAllPlayersPort;
import com.tournament.tournament_manager.domain.port.out.elo.SaveEloHistoryPort;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.EloHistoryEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.MatchEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.EloHistoryMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.PlayerMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.EloHistoryRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.MatchRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant les ports de sauvegarde ELO et historique.
 * Voir {@code PlayerJpaAdapter}/{@code MatchJpaAdapter} pour les principes de mapping
 * (préservation du {@code @Version}, résolution de références).
 */
@Component
public class EloJpaAdapter implements SaveAllPlayersPort, SaveEloHistoryPort, ExistsEloHistoryPort {

    private final PlayerRepository playerRepository;
    private final EloHistoryRepository eloHistoryRepository;
    private final MatchRepository matchRepository;
    private final PlayerMapper playerMapper;
    private final EloHistoryMapper eloHistoryMapper;

    public EloJpaAdapter(PlayerRepository playerRepository,
                         EloHistoryRepository eloHistoryRepository,
                         MatchRepository matchRepository,
                         PlayerMapper playerMapper,
                         EloHistoryMapper eloHistoryMapper) {
        this.playerRepository = playerRepository;
        this.eloHistoryRepository = eloHistoryRepository;
        this.matchRepository = matchRepository;
        this.playerMapper = playerMapper;
        this.eloHistoryMapper = eloHistoryMapper;
    }

    @Override
    public void saveAllPlayers(List<Player> players) {
        // Les joueurs ELO-mis-à-jour existent toujours déjà (id non null) : on charge
        // chaque entité existante pour préserver son @Version, plutôt que d'en construire
        // une neuve à l'aveugle (voir PlayerMapper).
        for (Player player : players) {
            PlayerEntity entity = playerRepository.findById(player.getId())
                    .orElseThrow(() -> new PlayerNotFoundException(player.getId()));
            playerMapper.updateEntity(entity, player);
            playerRepository.save(entity);
        }
    }

    @Override
    public void saveEloHistory(EloHistory eloHistory) {
        PlayerEntity playerRef = playerRepository.getReferenceById(eloHistory.getPlayer().getId());
        MatchEntity matchRef = matchRepository.getReferenceById(eloHistory.getMatch().getId());
        EloHistoryEntity entity = eloHistoryMapper.toNewEntity(eloHistory, playerRef, matchRef);
        eloHistoryRepository.save(entity);
    }

    @Override
    public boolean existsByMatchId(Long matchId) {
        return eloHistoryRepository.existsByMatchId(matchId);
    }
}
