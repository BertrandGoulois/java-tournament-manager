package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.port.in.match.GetMatchCommentaryUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.dto.response.MatchCommentaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du cas d'utilisation de consultation du commentaire de match.
 *
 * <p>Le commentaire est généré de façon asynchrone par {@code CommentaryListener}
 * après la fin du match. Ce service se contente de le lire depuis la base.
 */
@Service
@Transactional(readOnly = true)
public class MatchCommentaryService implements GetMatchCommentaryUseCase {

    private final LoadMatchPort loadMatchPort;

    public MatchCommentaryService(LoadMatchPort loadMatchPort) {
        this.loadMatchPort = loadMatchPort;
    }

    /**
     * Retourne le commentaire d'un match terminé.
     * Retourne un message d'attente si le commentaire n'est pas encore généré.
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