package com.tournament.tournament_manager.infrastructure.output.persistence.mapper;

import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RegistrationEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import org.springframework.stereotype.Component;

/**
 * Convertit entre le domaine pur {@link Registration} et sa contrepartie JPA
 * {@link RegistrationEntity}. Voir la Javadoc de {@code MatchMapper} pour le principe des
 * références résolues côté écriture.
 */
@Component
public class RegistrationMapper {

    private final PlayerMapper playerMapper;
    private final TournamentMapper tournamentMapper;

    public RegistrationMapper(PlayerMapper playerMapper, TournamentMapper tournamentMapper) {
        this.playerMapper = playerMapper;
        this.tournamentMapper = tournamentMapper;
    }

    public Registration toDomain(RegistrationEntity entity) {
        if (entity == null) {
            return null;
        }
        Registration registration = new Registration();
        registration.setId(entity.getId());
        registration.setRegisteredAt(entity.getRegisteredAt());
        registration.setTournament(tournamentMapper.toDomain(entity.getTournament()));
        registration.setPlayer(playerMapper.toDomain(entity.getPlayer()));
        return registration;
    }

    public RegistrationEntity toNewEntity(TournamentEntity tournamentRef, PlayerEntity playerRef) {
        RegistrationEntity entity = new RegistrationEntity();
        entity.setTournament(tournamentRef);
        entity.setPlayer(playerRef);
        return entity;
    }
}
