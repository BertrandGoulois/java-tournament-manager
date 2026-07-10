package com.tournament.tournament_manager.application.bracket;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvanceBracketServiceTest {

    @Mock
    private SaveTournamentPort saveTournamentPort;
    @Mock
    private LoadMatchByTournamentPort loadMatchByTournamentPort;
    @Mock
    private SaveMatchPort saveMatchPort;

    @InjectMocks
    private AdvanceBracketService advanceBracketService;

    @Test
    void advanceToNextRound_shouldDoNothing_whenNotAllMatchesFinished() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Match pendingMatch = new Match();
        pendingMatch.setStatus(MatchStatus.PENDING);
        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 4))
                .thenReturn(List.of(pendingMatch));
        advanceBracketService.advanceToNextRound(tournament, 4);
        verify(saveMatchPort, never()).saveMatch(any());
        verify(saveTournamentPort, never()).saveTournament(any());
    }

    @Test
    void advanceToNextRound_shouldFinishTournament_whenNextRoundLessThan2() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        Player winner = new Player();
        Match finishedMatch = new Match();
        finishedMatch.setStatus(MatchStatus.FINISHED);
        finishedMatch.setWinner(winner);
        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 2))
                .thenReturn(List.of(finishedMatch));
        advanceBracketService.advanceToNextRound(tournament, 2);
        assertEquals(TournamentStatus.FINISHED, tournament.getStatus());
        verify(saveTournamentPort, times(1)).saveTournament(tournament);
    }

    @Test
    void advanceToNextRound_shouldCreateNextRoundMatches_whenAllMatchesFinished() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Player winner1 = new Player();
        Player winner2 = new Player();
        Match match1 = new Match();
        match1.setStatus(MatchStatus.FINISHED);
        match1.setWinner(winner1);
        Match match2 = new Match();
        match2.setStatus(MatchStatus.FINISHED);
        match2.setWinner(winner2);
        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 4))
                .thenReturn(List.of(match1, match2));
        advanceBracketService.advanceToNextRound(tournament, 4);
        verify(saveMatchPort, times(1)).saveMatch(any(Match.class));
    }

    @Test
    void advanceToNextRound_shouldCreateByeMatch_whenOddNumberOfWinners() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Player winner1 = new Player();
        Player winner2 = new Player();
        Player winner3 = new Player();
        Match match1 = new Match();
        match1.setStatus(MatchStatus.FINISHED);
        match1.setWinner(winner1);
        Match match2 = new Match();
        match2.setStatus(MatchStatus.FINISHED);
        match2.setWinner(winner2);
        Match match3 = new Match();
        match3.setStatus(MatchStatus.FINISHED);
        match3.setWinner(winner3);
        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 8))
                .thenReturn(List.of(match1, match2, match3));
        advanceBracketService.advanceToNextRound(tournament, 8);
        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, atLeast(1)).saveMatch(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(m -> m.getPlayer2() == null));
    }
}