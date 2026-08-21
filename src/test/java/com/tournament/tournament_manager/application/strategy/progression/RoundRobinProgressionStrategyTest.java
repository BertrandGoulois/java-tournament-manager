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
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;

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
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, tournament, null, null, null);

        strategy.onMatchFinished(match, tournament);

        verify(checkTournamentCompletionUseCase, times(1)).checkCompletion(tournament);
    }
}