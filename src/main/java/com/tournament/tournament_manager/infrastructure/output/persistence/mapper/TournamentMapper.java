package com.tournament.tournament_manager.infrastructure.output.persistence.mapper;

import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import org.springframework.stereotype.Component;

/**
 * Convertit entre le domaine pur {@link Tournament} et sa contrepartie JPA
 * {@link TournamentEntity}. Voir la Javadoc de {@code PlayerMapper} pour le pattern
 * {@code updateEntity} (préservation du {@code @Version}), et celle de {@link Tournament}
 * pour {@code reconstitute} (aucun setter public sur le domaine, la reconstruction depuis
 * la persistance passe par cette factory dédiée).
 */
@Component
public class TournamentMapper {

    public Tournament toDomain(TournamentEntity entity) {
        if (entity == null) {
            return null;
        }
        return Tournament.reconstitute(
                entity.getId(),
                new TournamentName(entity.getName()),
                entity.getStatus(),
                entity.getFormat(),
                entity.getNumberOfGroups(),
                entity.getQualifiersPerGroup(),
                entity.getMaxPlayers(),
                entity.getCreatedAt(),
                entity.isDeleted(),
                entity.getDeletedAt()
        );
    }

    public TournamentEntity toNewEntity(Tournament tournament) {
        TournamentEntity entity = new TournamentEntity();
        updateEntity(entity, tournament);
        return entity;
    }

    public void updateEntity(TournamentEntity entity, Tournament tournament) {
        entity.setName(tournament.getName().value());
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
