package com.tournament.tournament_manager.infrastructure.strategy.progression;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.in.tournament.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentProgressionStrategy;
import org.springframework.stereotype.Component;

/**
 * Stratégie de progression pour le format {@link TournamentFormat#SINGLE_ELIMINATION}.
 * Avance le bracket au tour suivant après chaque match.
 */
@Component
public class SingleEliminationProgressionStrategy implements TournamentProgressionStrategy {

    private final AdvanceBracketUseCase advanceBracketUseCase;

    public SingleEliminationProgressionStrategy(AdvanceBracketUseCase advanceBracketUseCase) {
        this.advanceBracketUseCase = advanceBracketUseCase;
    }

    @Override
    public TournamentFormat supportedFormat() {
        return TournamentFormat.SINGLE_ELIMINATION;
    }

    @Override
    public void onMatchFinished(Match match, Tournament tournament) {
        advanceBracketUseCase.advanceToNextRound(tournament, match.getRound());
    }
}