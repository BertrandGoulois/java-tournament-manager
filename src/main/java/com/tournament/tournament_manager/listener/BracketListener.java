package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.in.tournament.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.CheckTournamentCompletionUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.GenerateKnockoutBracketFromGroupsUseCase;
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
 * Pour l'hybride ({@link TournamentFormat#GROUPS_THEN_KNOCKOUT}), route selon
 * la phase du match : un match de groupe déclenche la vérification d'achèvement
 * de la phase de groupes (et potentiellement la génération du bracket), un match
 * de bracket réutilise la logique d'avancement de l'élimination directe.
 */
@Component
public class BracketListener {

    private final AdvanceBracketUseCase advanceBracketUseCase;
    private final CheckTournamentCompletionUseCase checkTournamentCompletionUseCase;
    private final GenerateKnockoutBracketFromGroupsUseCase generateKnockoutBracketFromGroupsUseCase;
    private final LoadMatchPort loadMatchPort;

    public BracketListener(AdvanceBracketUseCase advanceBracketUseCase,
                           CheckTournamentCompletionUseCase checkTournamentCompletionUseCase,
                           GenerateKnockoutBracketFromGroupsUseCase generateKnockoutBracketFromGroupsUseCase,
                           LoadMatchPort loadMatchPort) {
        this.advanceBracketUseCase = advanceBracketUseCase;
        this.checkTournamentCompletionUseCase = checkTournamentCompletionUseCase;
        this.generateKnockoutBracketFromGroupsUseCase = generateKnockoutBracketFromGroupsUseCase;
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
        Tournament tournament = match.getTournament();

        switch (tournament.getFormat()) {
            case SINGLE_ELIMINATION -> advanceBracketUseCase.advanceToNextRound(tournament, match.getRound());
            case ROUND_ROBIN -> checkTournamentCompletionUseCase.checkCompletion(tournament);
            case GROUPS_THEN_KNOCKOUT -> handleGroupsThenKnockout(match, tournament);
        }
    }

    private void handleGroupsThenKnockout(Match match, Tournament tournament) {
        if (match.getGroupNumber() != null) {
            generateKnockoutBracketFromGroupsUseCase.checkGroupsCompletionAndGenerateBracket(tournament);
        } else {
            advanceBracketUseCase.advanceToNextRound(tournament, match.getRound());
        }
    }
}