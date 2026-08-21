package com.tournament.tournament_manager.application.strategy.progression;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.in.tournament.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.GenerateKnockoutBracketFromGroupsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;

@ExtendWith(MockitoExtension.class)
class GroupsThenKnockoutProgressionStrategyTest {

    @Mock
    private GenerateKnockoutBracketFromGroupsUseCase generateKnockoutBracketFromGroupsUseCase;
    @Mock
    private AdvanceBracketUseCase advanceBracketUseCase;

    @InjectMocks
    private GroupsThenKnockoutProgressionStrategy strategy;

    @Test
    void supportedFormat_shouldReturnGroupsThenKnockout() {
        assertEquals(TournamentFormat.GROUPS_THEN_KNOCKOUT, strategy.supportedFormat());
    }

    @Test
    void onMatchFinished_shouldCheckGroupsCompletion_whenGroupMatch() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Match match = Match.reconstitute(null, 0, 0, 1, MatchStatus.PENDING, null, null, null, null, null, null);

        strategy.onMatchFinished(match, tournament);

        verify(generateKnockoutBracketFromGroupsUseCase, times(1))
                .checkGroupsCompletionAndGenerateBracket(tournament);
        verify(advanceBracketUseCase, never()).advanceToNextRound(any(), anyInt());
    }

    @Test
    void onMatchFinished_shouldAdvanceBracket_whenKnockoutMatch() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Match match = Match.reconstitute(null, 2, 0, null, MatchStatus.PENDING, null, null, null, null, null, null);

        strategy.onMatchFinished(match, tournament);

        verify(advanceBracketUseCase, times(1)).advanceToNextRound(tournament, 2);
        verify(generateKnockoutBracketFromGroupsUseCase, never())
                .checkGroupsCompletionAndGenerateBracket(any());
    }
}