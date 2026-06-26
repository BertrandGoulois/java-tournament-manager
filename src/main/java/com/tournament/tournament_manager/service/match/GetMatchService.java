package com.tournament.tournament_manager.service.match;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.port.in.match.GetMatchUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.dto.response.match.MatchResponse;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : consultation d'un match.
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
     * @return la représentation du match
     * @throws MatchNotFoundException si le match n'existe pas
     */
    @Override
    public MatchResponse getMatchById(Long id) {
        return toResponse(loadMatchPort.loadMatch(id));
    }

    private MatchResponse toResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getRound(),
                match.getStatus(),
                match.getPlayedAt(),
                match.getTournament().getId(),
                match.getPlayer1().getId(),
                match.getPlayer2() != null ? match.getPlayer2().getId() : null,
                match.getWinner() != null ? match.getWinner().getId() : null
        );
    }
}