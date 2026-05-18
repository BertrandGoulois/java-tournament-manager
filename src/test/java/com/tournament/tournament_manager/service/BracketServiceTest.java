package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.*;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.NotFoundException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    @Mock
    private LoadTournamentPort loadTournamentPort;
    @Mock
    private SaveTournamentPort saveTournamentPort;
    @Mock
    private LoadRegistrationPort loadRegistrationPort;
    @Mock
    private SaveMatchPort saveMatchPort;
    @Mock
    private LoadMatchByTournamentPort loadMatchByTournamentPort;

    @InjectMocks
    private BracketService bracketService;

    @Test
    void startTournament_shouldThrowException_whenTournamentNotOpen() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);

        assertThrows(InvalidException.class, () -> bracketService.startTournament(1L));
    }

    @Test
    void startTournament_shouldThrowException_whenLessThanTwoPlayers() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);

        Registration registration = new Registration();
        registration.setPlayer(new Player());

        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(registration));

        assertThrows(InvalidException.class, () -> bracketService.startTournament(1L));
    }

    @Test
    void startTournament_shouldThrowException_whenTournamentDoesntExist() {
        when(loadTournamentPort.loadTournament(1L)).thenThrow(new TournamentNotFoundException(1L));
        assertThrows(NotFoundException.class, () -> bracketService.startTournament(1L));
    }

    @Test
    void startTournament_shouldCreateOneMatch_whenTwoPlayersRegistered() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);

        Registration reg1 = new Registration();
        reg1.setPlayer(new Player());
        Registration reg2 = new Registration();
        reg2.setPlayer(new Player());

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(reg1, reg2));

        bracketService.startTournament(1L);

        verify(saveMatchPort, times(1)).saveMatch(any(Match.class));
    }

    @Test
    void startTournament_shouldCreateTwoMatches_whenFourPlayersRegistered() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);

        List<Registration> registrations = List.of(
                registrationWithPlayer(), registrationWithPlayer(),
                registrationWithPlayer(), registrationWithPlayer()
        );

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(registrations);

        bracketService.startTournament(1L);

        verify(saveMatchPort, times(2)).saveMatch(any(Match.class));
    }

    @Test
    void startTournament_shouldSetTournamentInProgress_whenStarted() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);

        Registration reg1 = new Registration();
        reg1.setPlayer(new Player());
        Registration reg2 = new Registration();
        reg2.setPlayer(new Player());

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(reg1, reg2));

        bracketService.startTournament(1L);

        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(saveTournamentPort, times(1)).saveTournament(tournament);
    }

    @Test
    void startTournament_shouldCreateByeMatch_whenOddNumberOfPlayers() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);

        List<Registration> registrations = List.of(
                registrationWithPlayer(), registrationWithPlayer(), registrationWithPlayer()
        );

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(registrations);

        bracketService.startTournament(1L);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(2)).saveMatch(captor.capture());

        boolean hasByeMatch = captor.getAllValues().stream().anyMatch(m -> m.getPlayer2() == null);
        assertTrue(hasByeMatch);
    }

    @Test
    void advanceToNextRound_shouldDoNothing_whenNotAllMatchesFinished() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);

        Match pendingMatch = new Match();
        pendingMatch.setStatus(MatchStatus.PENDING);

        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 4))
                .thenReturn(List.of(pendingMatch));

        bracketService.advanceToNextRound(tournament, 4);

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

        bracketService.advanceToNextRound(tournament, 2);

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

        bracketService.advanceToNextRound(tournament, 4);

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

        bracketService.advanceToNextRound(tournament, 8);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, atLeast(1)).saveMatch(captor.capture());

        boolean hasByeMatch = captor.getAllValues().stream().anyMatch(m -> m.getPlayer2() == null);
        assertTrue(hasByeMatch);
    }

    private Registration registrationWithPlayer() {
        Registration reg = new Registration();
        reg.setPlayer(new Player());
        return reg;
    }
}