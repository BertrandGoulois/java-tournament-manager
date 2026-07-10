package com.tournament.tournament_manager.infrastructure.input.messaging;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.port.out.match.GenerateCommentaryPort;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveCommentaryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme les événements {@link MatchFinishedEvent} depuis le topic Kafka
 * {@code match-finished} et génère un commentaire narratif via LLM.
 *
 * <p>Le commentaire est généré de façon asynchrone après la fin du match
 * et persisté en base pour consultation ultérieure.
 * Les matchs de bye (sans {@code player2}) sont ignorés.
 */
@Slf4j
@Component
public class CommentaryListener {

    private final LoadMatchPort loadMatchPort;
    private final GenerateCommentaryPort generateCommentaryPort;
    private final SaveCommentaryPort saveCommentaryPort;

    public CommentaryListener(LoadMatchPort loadMatchPort,
                              GenerateCommentaryPort generateCommentaryPort,
                              SaveCommentaryPort saveCommentaryPort) {
        this.loadMatchPort = loadMatchPort;
        this.generateCommentaryPort = generateCommentaryPort;
        this.saveCommentaryPort = saveCommentaryPort;
    }

    /**
     * Génère et persiste un commentaire narratif pour un match terminé.
     * Ignoré si le match est un bye ({@code player2 == null}).
     *
     * @param event l'événement contenant l'identifiant du match terminé
     */
    @KafkaListener(topics = KafkaConfig.MATCH_FINISHED_TOPIC, groupId = KafkaConfig.COMMENTARY_GROUP)
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = loadMatchPort.loadMatch(event.matchId());
        if (match.getPlayer2() == null) return;

        try {
            int player1Elo = match.getPlayer1().getEloRating().value();
            int player2Elo = match.getPlayer2().getEloRating().value();
            String player1Name = match.getPlayer1().getUsername();
            String player2Name = match.getPlayer2().getUsername();
            String winnerName = match.getWinner().getUsername();

            double expectedWinner = 1.0 / (1 + Math.pow(10, (player2Elo - player1Elo) / 400.0));
            int winnerProb = (int) Math.round(
                    match.getWinner().equals(match.getPlayer1())
                            ? expectedWinner * 100
                            : (1 - expectedWinner) * 100
            );

            String prompt = String.format("""
                    Génère un commentaire sportif court (2-3 phrases) en français pour ce match de tournoi :
                    - Joueur 1 : %s (ELO %d)
                    - Joueur 2 : %s (ELO %d)
                    - Vainqueur : %s
                    - Probabilité de victoire du vainqueur avant le match : %d%%
                    - Round : %d
                    
                    Le commentaire doit être dynamique et narratif, comme un commentateur sportif.
                    """,
                    player1Name, player1Elo,
                    player2Name, player2Elo,
                    winnerName, winnerProb,
                    match.getRound()
            );

            String commentary = generateCommentaryPort.generateCommentary(prompt);
            saveCommentaryPort.saveCommentary(event.matchId(), commentary);
            log.info("Commentaire généré pour le match {}", event.matchId());

        } catch (Exception e) {
            log.error("Erreur lors de la génération du commentaire pour le match {} : {}",
                    event.matchId(), e.getMessage());
        }
    }
}