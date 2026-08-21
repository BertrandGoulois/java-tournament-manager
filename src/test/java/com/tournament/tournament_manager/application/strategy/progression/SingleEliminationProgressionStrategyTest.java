package com.tournament.tournament_manager.application.strategy.progression;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.in.tournament.AdvanceBracketUseCase;
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
class SingleEliminationProgressionStrategyTest {

    @Mock
    private AdvanceBracketUseCase advanceBracketUseCase;

    @InjectMocks
    private SingleEliminationProgressionStrategy strategy;

    @Test
    void supportedFormat_shouldReturnSingleElimination() {
        assertEquals(TournamentFormat.SINGLE_ELIMINATION, strategy.supportedFormat());
    }

    @Test
    void onMatchFinished_shouldCallAdvanceToNextRound() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Match match = Match.reconstitute(null, 4, 0, null, MatchStatus.PENDING, null, null, tournament, null, null, null);

        strategy.onMatchFinished(match, tournament);

        verify(advanceBracketUseCase, times(1)).advanceToNextRound(tournament, 4);
    }
}