package com.tournament.tournament_manager.application.match;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.port.in.match.GetMatchUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : consultation d'un match. Retourne un objet de domaine pur — voir la
 * Javadoc de {@code GetPlayerService}.
 */
@Service
@Transactional(readOnly = true)
public class GetMatchService implements GetMatchUseCase {

    private final LoadMatchPort loadMatchPort;

    public GetMatchService(LoadMatchPort loadMatchPort) {
        this.loadMatchPort = loadMatchPort;
    }

    /**
     * Retourne un match par son identifiant.
     *
     * @param id identifiant du match
     * @return le match
     * @throws MatchNotFoundException si le match n'existe pas
     */
    @Override
    public Match getMatchById(Long id) {
        return loadMatchPort.loadMatch(id);
    }
}
