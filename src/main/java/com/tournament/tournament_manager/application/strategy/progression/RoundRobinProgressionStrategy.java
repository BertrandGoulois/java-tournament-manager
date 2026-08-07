package com.tournament.tournament_manager.application.strategy.progression;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.in.tournament.CheckTournamentCompletionUseCase;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentProgressionStrategy;
import org.springframework.stereotype.Component;

/**
 * Stratégie de progression pour le format {@link TournamentFormat#ROUND_ROBIN}.
 * Vérifie si tous les matchs sont terminés et marque le tournoi comme FINISHED.
 */
@Component
public class RoundRobinProgressionStrategy implements TournamentProgressionStrategy {

    private final CheckTournamentCompletionUseCase checkTournamentCompletionUseCase;

    public RoundRobinProgressionStrategy(CheckTournamentCompletionUseCase checkTournamentCompletionUseCase) {
        this.checkTournamentCompletionUseCase = checkTournamentCompletionUseCase;
    }

    @Override
    public TournamentFormat supportedFormat() {
        return TournamentFormat.ROUND_ROBIN;
    }

    @Override
    public void onMatchFinished(Match match, Tournament tournament) {
        checkTournamentCompletionUseCase.checkCompletion(tournament);
    }
}