package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.port.in.UpdateEloUseCase;
import com.tournament.tournament_manager.domain.port.out.LoadMatchPort;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme les événements {@link MatchFinishedEvent} depuis le topic Kafka
 * {@code match-finished} et déclenche la mise à jour des classements ELO.
 *
 * <p>Les matchs de bye (sans {@code player2}) sont ignorés : aucun calcul ELO
 * n'est effectué pour une qualification automatique.
 */
@Component
public class EloListener {

    private final UpdateEloUseCase updateEloUseCase;
    private final LoadMatchPort loadMatchPort;

    public EloListener(UpdateEloUseCase updateEloUseCase,
                       LoadMatchPort loadMatchPort) {
        this.updateEloUseCase = updateEloUseCase;
        this.loadMatchPort = loadMatchPort;
    }

    /**
     * Récupère le match correspondant à l'événement et délègue
     * le calcul ELO à {@link UpdateEloUseCase}.
     * Ignoré si le match est un bye ({@code player2 == null}).
     *
     * @param event l'événement contenant l'identifiant du match terminé
     * @throws MatchNotFoundException si le match n'existe pas
     */
    @KafkaListener(topics = KafkaConfig.MATCH_FINISHED_TOPIC, groupId = "elo-group")
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = loadMatchPort.loadMatch(event.matchId());
        if (match.getPlayer2() == null) return;
        updateEloUseCase.updateElo(match);
    }
}