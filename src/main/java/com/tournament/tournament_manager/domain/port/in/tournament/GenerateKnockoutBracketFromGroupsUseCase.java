package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;

/**
 * Cas d'utilisation : vérifie si la phase de groupes d'un tournoi
 * {@code GROUPS_THEN_KNOCKOUT} est terminée, et génère le bracket final
 * entre qualifiés le cas échéant.
 */
public interface GenerateKnockoutBracketFromGroupsUseCase {

    /**
     * Vérifie l'achèvement de la phase de groupes et déclenche la génération
     * du bracket knockout si tous les matchs de groupe sont terminés.
     *
     * @param tournament le tournoi à vérifier
     */
    void checkGroupsCompletionAndGenerateBracket(Tournament tournament);
}