package com.tournament.tournament_manager.domain.port.in.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;

/**
 * Cas d'utilisation : vérifie si un tournoi round-robin est terminé
 * (tous ses matchs ont été joués) et le marque {@code FINISHED} le cas échéant.
 */
public interface CheckTournamentCompletionUseCase {

    /**
     * Vérifie l'achèvement du tournoi et met à jour son statut si nécessaire.
     *
     * @param tournament le tournoi à vérifier
     */
    void checkCompletion(Tournament tournament);
}