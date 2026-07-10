package com.tournament.tournament_manager.infrastructure.input.messaging;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.in.tournament.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.CheckTournamentCompletionUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.GenerateKnockoutBracketFromGroupsUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import com.tournament.tournament_manager.application.strategy.progression.GroupsThenKnockoutProgressionStrategy;
import com.tournament.tournament_manager.application.strategy.progression.RoundRobinProgressionStrategy;
import com.tournament.tournament_manager.application.strategy.progression.SingleEliminationProgressionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BracketListenerTest {

    @Mock
    private AdvanceBracketUseCase advanceBracketUseCase;
    @Mock
    private CheckTournamentCompletionUseCase checkTournamentCompletionUseCase;
    @Mock
    private GenerateKnockoutBracketFromGroupsUseCase generateKnockoutBracketFromGroupsUseCase;
    @Mock
    private LoadMatchPort loadMatchPort;

    private BracketListener bracketListener;

    @BeforeEach
    void setUp() {
        var strategies = List.of(
                new SingleEliminationProgressionStrategy(advanceBracketUseCase),
                new RoundRobinProgressionStrategy(checkTournamentCompletionUseCase),
                new GroupsThenKnockoutProgressionStrategy(generateKnockoutBracketFromGroupsUseCase, advanceBracketUseCase)
        );
        bracketListener = new BracketListener(loadMatchPort, strategies);
    }

    @Test
    void onMatchFinished_shouldCallAdvanceToNextRound_whenSingleElimination() {
        Tournament tournament = new Tournament();
        tournament.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        Match match = new Match();
        match.setTournament(tournament);
        match.setRound(4);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        bracketListener.onMatchFinished(new MatchFinishedEvent(1L));

        verify(advanceBracketUseCase, times(1)).advanceToNextRound(tournament, 4);
        verify(checkTournamentCompletionUseCase, never()).checkCompletion(any());
    }

    @Test
    void onMatchFinished_shouldCallCheckCompletion_whenRoundRobin() {
        Tournament tournament = new Tournament();
        tournament.setFormat(TournamentFormat.ROUND_ROBIN);
        Match match = new Match();
        match.setTournament(tournament);
        match.setRound(1);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        bracketListener.onMatchFinished(new MatchFinishedEvent(1L));

        verify(checkTournamentCompletionUseCase, times(1)).checkCompletion(tournament);
        verify(advanceBracketUseCase, never()).advanceToNextRound(any(), anyInt());
    }

    @Test
    void onMatchFinished_shouldThrow_whenMatchNotFound() {
        when(loadMatchPort.loadMatch(99L)).thenThrow(new MatchNotFoundException(99L));

        assertThrows(MatchNotFoundException.class,
                () -> bracketListener.onMatchFinished(new MatchFinishedEvent(99L)));
    }

    @Test
    void onMatchFinished_shouldCheckGroupsCompletion_whenGroupMatchInGroupsThenKnockout() {
        Tournament tournament = new Tournament();
        tournament.setFormat(TournamentFormat.GROUPS_THEN_KNOCKOUT);
        Match match = new Match();
        match.setTournament(tournament);
        match.setGroupNumber(1);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        bracketListener.onMatchFinished(new MatchFinishedEvent(1L));

        verify(generateKnockoutBracketFromGroupsUseCase, times(1)).checkGroupsCompletionAndGenerateBracket(tournament);
        verify(advanceBracketUseCase, never()).advanceToNextRound(any(), anyInt());
    }

    @Test
    void onMatchFinished_shouldAdvanceBracket_whenKnockoutMatchInGroupsThenKnockout() {
        Tournament tournament = new Tournament();
        tournament.setFormat(TournamentFormat.GROUPS_THEN_KNOCKOUT);
        Match match = new Match();
        match.setTournament(tournament);
        match.setGroupNumber(null);
        match.setRound(2);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        bracketListener.onMatchFinished(new MatchFinishedEvent(1L));

        verify(advanceBracketUseCase, times(1)).advanceToNextRound(tournament, 2);
        verify(generateKnockoutBracketFromGroupsUseCase, never()).checkGroupsCompletionAndGenerateBracket(any());
    }
}