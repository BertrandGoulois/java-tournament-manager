package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.port.in.tournament.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme les événements {@link MatchFinishedEvent} depuis le topic Kafka
 * {@code match-finished} et tente de faire progresser le bracket au tour suivant.
 *
 * <p>L'avancement n'a lieu que si tous les matchs du round en cours sont terminés —
 * la logique de vérification est déléguée à {@link AdvanceBracketUseCase}.
 */
@Component
public class BracketListener {

    private final AdvanceBracketUseCase advanceBracketUseCase;
    private final LoadMatchPort loadMatchPort;

    public BracketListener(AdvanceBracketUseCase advanceBracketUseCase,
                           LoadMatchPort loadMatchPort) {
        this.advanceBracketUseCase = advanceBracketUseCase;
        this.loadMatchPort = loadMatchPort;
    }

    /**
     * Récupère le match correspondant à l'événement et délègue
     * l'avancement du bracket à {@link AdvanceBracketUseCase}.
     *
     * @param event l'événement contenant l'identifiant du match terminé
     */
    @KafkaListener(topics = KafkaConfig.MATCH_FINISHED_TOPIC, groupId = "bracket-group")
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = loadMatchPort.loadMatch(event.matchId());
        advanceBracketUseCase.advanceToNextRound(match.getTournament(), match.getRound());
    }
}