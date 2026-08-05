package com.tournament.tournament_manager.application.match;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.port.in.match.GetMatchCommentaryUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.dto.response.match.MatchCommentaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : consultation du commentaire d'un match.
 */
@Service
@Transactional(readOnly = true)
public class GetMatchCommentaryService implements GetMatchCommentaryUseCase {

    private final LoadMatchPort loadMatchPort;

    public GetMatchCommentaryService(LoadMatchPort loadMatchPort) {
        this.loadMatchPort = loadMatchPort;
    }

    /**
     * Retourne le commentaire d'un match terminé.
     * Retourne un message d'attente si le commentaire n'est pas encore généré
     * (génération asynchrone via {@code CommentaryListener}, voir sa Javadoc).
     *
     * @param matchId identifiant du match
     * @return le commentaire du match
     */
    @Override
    public MatchCommentaryResponse getMatchCommentary(Long matchId) {
        Match match = loadMatchPort.loadMatch(matchId);

        String commentary = match.getCommentary() != null
                ? match.getCommentary()
                : "Commentaire en cours de génération...";

        return new MatchCommentaryResponse(matchId, commentary);
    }
}