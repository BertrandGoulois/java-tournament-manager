package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.EloHistory;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.port.out.player.*;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.EloHistoryMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.PlayerMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.EloHistoryRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.MatchRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant les ports de chargement, sauvegarde
 * et consultation des joueurs.
 *
 * <p>Convertit entre le domaine pur {@link Player} et l'entité JPA {@code PlayerEntity} via
 * {@link PlayerMapper} — voir sa Javadoc pour le traitement du verrouillage optimiste
 * ({@code @Version}) lors des mises à jour.
 */
@Component
public class PlayerJpaAdapter implements LoadPlayerPort, SavePlayerPort,
        ExistsPlayerPort, LoadAllPlayersPort, CountMatchesByPlayerPort, LoadEloHistoryPort, SoftDeletePlayerPort {

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final EloHistoryRepository eloHistoryRepository;
    private final PlayerMapper playerMapper;
    private final EloHistoryMapper eloHistoryMapper;

    public PlayerJpaAdapter(PlayerRepository playerRepository,
                            MatchRepository matchRepository,
                            EloHistoryRepository eloHistoryRepository,
                            PlayerMapper playerMapper,
                            EloHistoryMapper eloHistoryMapper) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.eloHistoryRepository = eloHistoryRepository;
        this.playerMapper = playerMapper;
        this.eloHistoryMapper = eloHistoryMapper;
    }

    @Override
    public Player loadPlayer(Long id) {
        PlayerEntity entity = playerRepository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
        return playerMapper.toDomain(entity);
    }

    @Override
    public Player savePlayer(Player player) {
        PlayerEntity entity;
        if (player.getId() != null) {
            // Mise à jour : on charge l'entité existante (version intacte gérée par
            // Hibernate) plutôt que d'en reconstruire une neuve à l'aveugle — voir la
            // Javadoc de PlayerMapper.
            entity = playerRepository.findById(player.getId())
                    .orElseThrow(() -> new PlayerNotFoundException(player.getId()));
            playerMapper.updateEntity(entity, player);
        } else {
            entity = playerMapper.toNewEntity(player);
        }
        PlayerEntity saved = playerRepository.save(entity);
        return playerMapper.toDomain(saved);
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
        return playerRepository.findAll(pageable).map(playerMapper::toDomain);
    }

    @Override
    public long countByPlayer(Long playerId) {
        return matchRepository.countFinishedRealMatchesByPlayer(playerId);
    }

    @Override
    public long countWinsByPlayer(Long playerId) {
        return matchRepository.countRealWinsByPlayer(playerId);
    }

    @Override
    public List<EloHistory> loadByPlayerIdOrderByDateDesc(Long playerId) {
        return eloHistoryRepository.findByPlayerIdOrderByCreatedAtDesc(playerId).stream()
                .map(eloHistoryMapper::toDomain)
                .toList();
    }

    @Override
    public void softDeletePlayer(Player player) {
        savePlayer(player);
    }
}
