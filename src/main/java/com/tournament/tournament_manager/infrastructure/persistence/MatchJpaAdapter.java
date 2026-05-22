package com.tournament.tournament_manager.infrastructure.persistence;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant les ports de chargement et sauvegarde des matchs.
 * Fait le lien entre le domaine et la couche de persistance Spring Data.
 */
@Component
public class MatchJpaAdapter implements LoadMatchPort, SaveMatchPort, LoadMatchByTournamentPort {

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

    @Override
    public List<Match> loadByTournamentIdAndRound(Long tournamentId, int round) {
        return matchRepository.findByTournamentIdAndRound(tournamentId, round);
    }
}