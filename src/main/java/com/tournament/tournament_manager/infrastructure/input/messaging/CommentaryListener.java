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
 *
 * <p>La probabilité de victoire "avant match" est calculée à partir des ELO portés par
 * l'événement ({@link MatchFinishedEvent#player1EloBefore()}), jamais relus sur
 * {@code match.getPlayerX().getEloRating()} : {@code EloListener} consomme le même
 * événement en parallèle, dans un consumer group indépendant, et peut avoir déjà modifié
 * ces ELO au moment où ce listener s'exécute — les relire donnerait un résultat non
 * déterministe selon lequel des deux consumers "gagne la course".
 *
 * <p>Aucun {@code catch} générique ici : toute exception (échec OpenAI, match introuvable...)
 * doit remonter jusqu'au mécanisme de retry/DLT configuré dans {@code KafkaConfig}, comme
 * pour les autres listeners de ce topic. Un {@code catch} qui journalise et avale
 * l'exception empêcherait ce mécanisme de jamais s'appliquer aux échecs de commentaire.
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
     * Ignoré si le match est un bye ({@code player2 == null}) ou si un commentaire a déjà
     * été généré pour ce match (idempotence face à la redelivery Kafka — chaque redelivery
     * non filtrée serait un appel OpenAI facturé en plus, pour rien).
     *
     * @param event l'événement contenant l'identifiant du match terminé et les ELO d'avant match
     */
    @KafkaListener(topics = KafkaConfig.MATCH_FINISHED_TOPIC, groupId = KafkaConfig.COMMENTARY_GROUP)
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = loadMatchPort.loadMatch(event.matchId());
        if (match.getPlayer2() == null) {
            log.debug("Match bye ignoré pour le commentaire [matchId={}]", event.matchId());
            return;
        }
        if (match.getCommentary() != null) {
            log.warn("Commentaire déjà généré pour ce match, ignoré [matchId={}]", event.matchId());
            return;
        }

        int player1Elo = event.player1EloBefore();
        int player2Elo = event.player2EloBefore();
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
                - Joueur 1 : <player1_name>%s</player1_name> (ELO %d)
                - Joueur 2 : <player2_name>%s</player2_name> (ELO %d)
                - Vainqueur : <winner_name>%s</winner_name>
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
    }
}