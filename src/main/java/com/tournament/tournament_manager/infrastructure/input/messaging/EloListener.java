package com.tournament.tournament_manager.infrastructure.input.messaging;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.port.in.elo.UpdateEloUseCase;
import com.tournament.tournament_manager.domain.port.out.elo.ExistsEloHistoryPort;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme les événements {@link MatchFinishedEvent} depuis le topic Kafka
 * {@code match-finished} et déclenche la mise à jour des classements ELO.
 *
 * <p>Les matchs de bye (sans {@code player2}) sont ignorés : aucun calcul ELO
 * n'est effectué pour une qualification automatique.
 */
@Slf4j
@Component
public class EloListener {

    private final UpdateEloUseCase updateEloUseCase;
    private final LoadMatchPort loadMatchPort;
    private final ExistsEloHistoryPort existsEloHistoryPort;

    public EloListener(UpdateEloUseCase updateEloUseCase,
                       LoadMatchPort loadMatchPort,
                       ExistsEloHistoryPort existsEloHistoryPort) {
        this.updateEloUseCase = updateEloUseCase;
        this.loadMatchPort = loadMatchPort;
        this.existsEloHistoryPort = existsEloHistoryPort;
    }

    /**
     * Récupère le match correspondant à l'événement et délègue
     * le calcul ELO à {@link UpdateEloUseCase}.
     * Ignoré si le match est un bye ({@code player2 == null}).
     *
     * @param event l'événement contenant l'identifiant du match terminé
     * @throws MatchNotFoundException si le match n'existe pas
     */
    @KafkaListener(topics = KafkaConfig.MATCH_FINISHED_TOPIC, groupId = KafkaConfig.ELO_GROUP)
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = loadMatchPort.loadMatch(event.matchId());
        if (match.getPlayer2() == null){
            log.debug("Match bye ignoré pour le calcul ELO [matchId={}]", event.matchId());
            return;
        }
        if (existsEloHistoryPort.existsByMatchId(event.matchId())){
            log.warn("ELO déjà calculé pour ce match, ignoré [matchId={}]", event.matchId());
            return;
        }
        log.info("Mise à jour ELO [matchId={}, player1='{}', player2='{}']",
                event.matchId(),
                match.getPlayer1().getUsername(),
                match.getPlayer2().getUsername());
        updateEloUseCase.updateElo(match);
    }
}