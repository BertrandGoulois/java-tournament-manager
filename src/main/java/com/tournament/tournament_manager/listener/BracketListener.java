package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.in.tournament.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.CheckTournamentCompletionUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme les événements {@link MatchFinishedEvent} depuis le topic Kafka
 * {@code match-finished} et fait progresser le tournoi selon son format.
 *
 * <p>Pour l'élimination directe ({@link TournamentFormat#SINGLE_ELIMINATION}),
 * délègue l'avancement au tour suivant à {@link AdvanceBracketUseCase}.
 * Pour le round-robin ({@link TournamentFormat#ROUND_ROBIN}), vérifie simplement
 * si tous les matchs sont terminés via {@link CheckTournamentCompletionUseCase}.
 */
@Component
public class BracketListener {

    private final AdvanceBracketUseCase advanceBracketUseCase;
    private final CheckTournamentCompletionUseCase checkTournamentCompletionUseCase;
    private final LoadMatchPort loadMatchPort;

    public BracketListener(AdvanceBracketUseCase advanceBracketUseCase,
                           CheckTournamentCompletionUseCase checkTournamentCompletionUseCase,
                           LoadMatchPort loadMatchPort) {
        this.advanceBracketUseCase = advanceBracketUseCase;
        this.checkTournamentCompletionUseCase = checkTournamentCompletionUseCase;
        this.loadMatchPort = loadMatchPort;
    }

    /**
     * Récupère le match correspondant à l'événement et fait progresser
     * le tournoi selon son format.
     *
     * @param event l'événement contenant l'identifiant du match terminé
     */
    @KafkaListener(topics = KafkaConfig.MATCH_FINISHED_TOPIC, groupId = KafkaConfig.BRACKET_GROUP)
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = loadMatchPort.loadMatch(event.matchId());

        // TODO: étendre ce switch pour TournamentFormat.GROUPS_THEN_KNOCKOUT
        // (phase de groupes en round-robin, puis bracket généré à partir des qualifiés)
        if (match.getTournament().getFormat() == TournamentFormat.SINGLE_ELIMINATION) {
            advanceBracketUseCase.advanceToNextRound(match.getTournament(), match.getRound());
        } else if (match.getTournament().getFormat() == TournamentFormat.ROUND_ROBIN) {
            checkTournamentCompletionUseCase.checkCompletion(match.getTournament());
        }
    }
}