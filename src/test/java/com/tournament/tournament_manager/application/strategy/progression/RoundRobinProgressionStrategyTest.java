package com.tournament.tournament_manager.application.strategy.progression;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.in.tournament.CheckTournamentCompletionUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoundRobinProgressionStrategyTest {

    @Mock
    private CheckTournamentCompletionUseCase checkTournamentCompletionUseCase;

    @InjectMocks
    private RoundRobinProgressionStrategy strategy;

    @Test
    void supportedFormat_shouldReturnRoundRobin() {
        assertEquals(TournamentFormat.ROUND_ROBIN, strategy.supportedFormat());
    }

    @Test
    void onMatchFinished_shouldCallCheckCompletion() {
        Tournament tournament = new Tournament();
        Match match = new Match();
        match.setTournament(tournament);

        strategy.onMatchFinished(match, tournament);

        verify(checkTournamentCompletionUseCase, times(1)).checkCompletion(tournament);
    }
}