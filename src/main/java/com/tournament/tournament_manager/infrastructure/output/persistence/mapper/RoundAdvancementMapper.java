package com.tournament.tournament_manager.infrastructure.output.persistence.mapper;

import com.tournament.tournament_manager.domain.model.RoundAdvancement;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RoundAdvancementEntity;
import org.springframework.stereotype.Component;

@Component
public class RoundAdvancementMapper {

    public RoundAdvancement toDomain(RoundAdvancementEntity entity) {
        if (entity == null) {
            return null;
        }
        RoundAdvancement advancement = new RoundAdvancement();
        advancement.setId(entity.getId());
        advancement.setTournamentId(entity.getTournamentId());
        advancement.setRound(entity.getRound());
        advancement.setCreatedAt(entity.getCreatedAt());
        return advancement;
    }

    public RoundAdvancementEntity toNewEntity(Long tournamentId, int round) {
        RoundAdvancementEntity entity = new RoundAdvancementEntity();
        entity.setTournamentId(tournamentId);
        entity.setRound(round);
        return entity;
    }
}
