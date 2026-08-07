package com.tournament.tournament_manager.infrastructure.output.persistence.mapper;

import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import org.springframework.stereotype.Component;

/**
 * Convertit entre le domaine pur {@link Tournament} et sa contrepartie JPA
 * {@link TournamentEntity}. Voir la Javadoc de {@code PlayerMapper} pour le pattern
 * {@code updateEntity} (préservation du {@code @Version}).
 */
@Component
public class TournamentMapper {

    public Tournament toDomain(TournamentEntity entity) {
        if (entity == null) {
            return null;
        }
        Tournament tournament = new Tournament();
        tournament.setId(entity.getId());
        tournament.setName(entity.getName());
        tournament.setStatus(entity.getStatus());
        tournament.setFormat(entity.getFormat());
        tournament.setNumberOfGroups(entity.getNumberOfGroups());
        tournament.setQualifiersPerGroup(entity.getQualifiersPerGroup());
        tournament.setMaxPlayers(entity.getMaxPlayers());
        tournament.setCreatedAt(entity.getCreatedAt());
        tournament.setDeleted(entity.isDeleted());
        tournament.setDeletedAt(entity.getDeletedAt());
        return tournament;
    }

    public TournamentEntity toNewEntity(Tournament tournament) {
        TournamentEntity entity = new TournamentEntity();
        updateEntity(entity, tournament);
        return entity;
    }

    public void updateEntity(TournamentEntity entity, Tournament tournament) {
        entity.setName(tournament.getName());
        entity.setStatus(tournament.getStatus());
        entity.setFormat(tournament.getFormat());
        entity.setNumberOfGroups(tournament.getNumberOfGroups());
        entity.setQualifiersPerGroup(tournament.getQualifiersPerGroup());
        entity.setMaxPlayers(tournament.getMaxPlayers());
        entity.setDeleted(tournament.isDeleted());
        entity.setDeletedAt(tournament.getDeletedAt());
        if (tournament.getCreatedAt() != null) {
            entity.setCreatedAt(tournament.getCreatedAt());
        }
    }
}
