package com.tournament.tournament_manager.domain.event;

/**
 * Événement publié sur le topic Kafka {@code match-finished}
 * à chaque fois qu'un match est terminé.
 *
 * <p>Consommé indépendamment par quatre listeners :
 * <ul>
 *   <li>{@code EloListener} — met à jour les classements ELO des deux joueurs</li>
 *   <li>{@code BracketListener} — fait avancer le bracket au tour suivant</li>
 *   <li>{@code WebSocketListener} — notifie les clients connectés en temps réel</li>
 *   <li>{@code CommentaryListener} — génère un commentaire narratif via LLM</li>
 * </ul>
 *
 * @param matchId          identifiant du match terminé
 * @param player1EloBefore ELO de {@code player1} juste avant ce match, capturé au moment de
 *                          la publication — avant toute mise à jour ELO asynchrone par
 *                          {@code EloListener}, qui tourne dans un consumer group indépendant
 *                          et pourrait sinon avoir déjà modifié l'ELO au moment où
 *                          {@code CommentaryListener} traite le même événement. {@code 0} pour
 *                          un bye (donnée sans sens, aucun second joueur).
 * @param player2EloBefore ELO de {@code player2} juste avant ce match, {@code 0} pour un bye.
 */
public record MatchFinishedEvent(Long matchId, int player1EloBefore, int player2EloBefore) {
}
