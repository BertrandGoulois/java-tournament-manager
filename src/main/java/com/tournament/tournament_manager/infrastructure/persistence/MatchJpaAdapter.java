package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.port.out.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.SaveMatchPort;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import org.springframework.stereotype.Component;

/**
 * Adapter JPA implémentant les ports de chargement et sauvegarde des matchs.
 * Fait le lien entre le domaine et la couche de persistance Spring Data.
 */
@Component
public class MatchJpaAdapter implements LoadMatchPort, SaveMatchPort {

    private final MatchRepository matchRepository;

    public MatchJpaAdapter(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @Override
    public Match loadMatch(Long id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException(id));
    }

    @Override
    public Match saveMatch(Match match) {
        return matchRepository.save(match);
    }
}