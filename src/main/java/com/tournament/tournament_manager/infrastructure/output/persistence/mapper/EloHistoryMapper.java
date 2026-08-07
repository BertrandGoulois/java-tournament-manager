package com.tournament.tournament_manager.infrastructure.output.persistence.mapper;

import com.tournament.tournament_manager.domain.model.EloHistory;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.EloHistoryEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.MatchEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import org.springframework.stereotype.Component;

/**
 * Convertit entre le domaine pur {@link EloHistory} et sa contrepartie JPA
 * {@link EloHistoryEntity}. Voir la Javadoc de {@code MatchMapper} pour le principe des
 * références résolues côté écriture.
 */
@Component
public class EloHistoryMapper {

    private final PlayerMapper playerMapper;
    private final MatchMapper matchMapper;

    public EloHistoryMapper(PlayerMapper playerMapper, MatchMapper matchMapper) {
        this.playerMapper = playerMapper;
        this.matchMapper = matchMapper;
    }

    public EloHistory toDomain(EloHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        EloHistory history = new EloHistory();
        history.setId(entity.getId());
        history.setEloChange(entity.getEloChange());
        history.setEloAfter(entity.getEloAfter());
        history.setCreatedAt(entity.getCreatedAt());
        history.setPlayer(playerMapper.toDomain(entity.getPlayer()));
        history.setMatch(matchMapper.toDomain(entity.getMatch()));
        return history;
    }

    public EloHistoryEntity toNewEntity(EloHistory history, PlayerEntity playerRef, MatchEntity matchRef) {
        EloHistoryEntity entity = new EloHistoryEntity();
        entity.setEloChange(history.getEloChange());
        entity.setEloAfter(history.getEloAfter());
        entity.setPlayer(playerRef);
        entity.setMatch(matchRef);
        return entity;
    }
}
