package com.tournament.tournament_manager.domain.port.out.match;

/**
 * Port sortant : sauvegarde du commentaire d'un match en persistance.
 */
public interface SaveCommentaryPort {

    /**
     * Sauvegarde le commentaire généré pour un match.
     *
     * @param matchId   identifiant du match
     * @param commentary le commentaire généré par le LLM
     */
    void saveCommentary(Long matchId, String commentary);
}