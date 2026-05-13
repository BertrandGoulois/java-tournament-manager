package com.tournament.tournament_manager.domain.event;

/**
 * Événement publié sur le topic Kafka {@code match-finished}
 * à chaque fois qu'un match est terminé.
 *
 * <p>Consommé indépendamment par trois listeners :
 * <ul>
 *   <li>{@code EloListener} — met à jour les classements ELO des deux joueurs</li>
 *   <li>{@code BracketListener} — fait avancer le bracket au tour suivant</li>
 *   <li>{@code WebSocketListener} — notifie les clients connectés en temps réel</li>
 * </ul>
 *
 * @param matchId identifiant du match terminé
 */
public record MatchFinishedEvent(Long matchId) {
}
