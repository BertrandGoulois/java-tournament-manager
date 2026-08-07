package com.tournament.tournament_manager.application.strategy.progression;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.in.tournament.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.GenerateKnockoutBracketFromGroupsUseCase;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentProgressionStrategy;
import org.springframework.stereotype.Component;

/**
 * Stratégie de progression pour le format {@link TournamentFormat#GROUPS_THEN_KNOCKOUT}.
 *
 * <p>Route selon la nature du match :
 * un match de groupe ({@code groupNumber != null}) déclenche la vérification
 * d'achèvement de la phase de groupes et potentiellement la génération du bracket ;
 * un match de bracket ({@code groupNumber == null}) avance le bracket au tour suivant.
 */
@Component
public class GroupsThenKnockoutProgressionStrategy implements TournamentProgressionStrategy {

    private final GenerateKnockoutBracketFromGroupsUseCase generateKnockoutBracketFromGroupsUseCase;
    private final AdvanceBracketUseCase advanceBracketUseCase;

    public GroupsThenKnockoutProgressionStrategy(
            GenerateKnockoutBracketFromGroupsUseCase generateKnockoutBracketFromGroupsUseCase,
            AdvanceBracketUseCase advanceBracketUseCase) {
        this.generateKnockoutBracketFromGroupsUseCase = generateKnockoutBracketFromGroupsUseCase;
        this.advanceBracketUseCase = advanceBracketUseCase;
    }

    @Override
    public TournamentFormat supportedFormat() {
        return TournamentFormat.GROUPS_THEN_KNOCKOUT;
    }

    @Override
    public void onMatchFinished(Match match, Tournament tournament) {
        if (match.getGroupNumber() != null) {
            generateKnockoutBracketFromGroupsUseCase.checkGroupsCompletionAndGenerateBracket(tournament);
        } else {
            advanceBracketUseCase.advanceToNextRound(tournament, match.getRound());
        }
    }
}