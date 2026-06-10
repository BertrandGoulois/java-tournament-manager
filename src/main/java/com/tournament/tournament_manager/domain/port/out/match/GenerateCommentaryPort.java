package com.tournament.tournament_manager.domain.port.out.match;

/**
 * Port sortant : génération de commentaire via un LLM externe.
 */
public interface GenerateCommentaryPort {

    /**
     * Génère un commentaire narratif à partir d'un prompt.
     *
     * @param prompt le prompt décrivant le match
     * @return le commentaire généré
     */
    String generateCommentary(String prompt);
}