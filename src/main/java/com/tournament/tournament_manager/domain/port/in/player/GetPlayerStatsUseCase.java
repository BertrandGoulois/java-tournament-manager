package com.tournament.tournament_manager.domain.port.in.player;

import com.tournament.tournament_manager.dto.response.PlayerStatsResponse;

/**
 * Port entrant : cas d'utilisation pour consulter les statistiques d'un joueur.
 */
public interface GetPlayerStatsUseCase {

    /**
     * Retourne les statistiques complètes d'un joueur : matchs joués,
     * victoires, défaites, win rate et historique ELO.
     *
     * <p>Le résultat est mis en cache Redis ({@code playerStats}) par identifiant joueur.
     *
     * @param id identifiant du joueur
     * @return les statistiques du joueur
     * @throws com.tournament.tournament_manager.exception.PlayerNotFoundException si le joueur n'existe pas
     */
    PlayerStatsResponse getPlayerStats(Long id);
}