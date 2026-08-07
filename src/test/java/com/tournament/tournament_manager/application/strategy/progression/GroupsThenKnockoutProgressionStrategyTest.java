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
        Tournament tournament = new Tournament();
        Match match = new Match();
        match.setGroupNumber(1);

        strategy.onMatchFinished(match, tournament);

        verify(generateKnockoutBracketFromGroupsUseCase, times(1))
                .checkGroupsCompletionAndGenerateBracket(tournament);
        verify(advanceBracketUseCase, never()).advanceToNextRound(any(), anyInt());
    }

    @Test
    void onMatchFinished_shouldAdvanceBracket_whenKnockoutMatch() {
        Tournament tournament = new Tournament();
        Match match = new Match();
        match.setGroupNumber(null);
        match.setRound(2);

        strategy.onMatchFinished(match, tournament);

        verify(advanceBracketUseCase, times(1)).advanceToNextRound(tournament, 2);
        verify(generateKnockoutBracketFromGroupsUseCase, never())
                .checkGroupsCompletionAndGenerateBracket(any());
    }
}