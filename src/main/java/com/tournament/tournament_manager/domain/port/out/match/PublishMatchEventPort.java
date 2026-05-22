package com.tournament.tournament_manager.domain.port.out.match;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;

/**
 * Port sortant : publication d'événements liés aux matchs.
 */
public interface PublishMatchEventPort {

    /**
     * Publie un événement de fin de match sur le bus de messages.
     * Les listeners abonnés (ELO, bracket, WebSocket) réagissent de façon indépendante.
     *
     * @param event l'événement contenant l'identifiant du match terminé
     */
    void publishMatchFinished(MatchFinishedEvent event);
}