package com.tournament.tournament_manager.service.bracket;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartTournamentServiceTest {

    @Mock
    private LoadTournamentPort loadTournamentPort;
    @Mock
    private SaveTournamentPort saveTournamentPort;
    @Mock
    private LoadRegistrationPort loadRegistrationPort;
    @Mock
    private SaveMatchPort saveMatchPort;

    @InjectMocks
    private StartTournamentService startTournamentService;

    @Test
    void startTournament_shouldThrowException_whenTournamentNotOpen() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        assertThrows(InvalidException.class, () -> startTournamentService.startTournament(1L));
    }

    @Test
    void startTournament_shouldThrowException_whenLessThanTwoPlayers() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        Registration registration = new Registration();
        registration.setPlayer(new Player());
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(registration));
        assertThrows(InvalidException.class, () -> startTournamentService.startTournament(1L));
    }

    @Test
    void startTournament_shouldThrowException_whenTournamentDoesntExist() {
        when(loadTournamentPort.loadTournament(1L)).thenThrow(new TournamentNotFoundException(1L));
        assertThrows(NotFoundException.class, () -> startTournamentService.startTournament(1L));
    }

    @Test
    void startTournament_shouldCreateOneMatch_whenTwoPlayersRegistered() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(
                registrationWithPlayer(), registrationWithPlayer()
        ));
        startTournamentService.startTournament(1L);
        verify(saveMatchPort, times(1)).saveMatch(any(Match.class));
    }

    @Test
    void startTournament_shouldCreateTwoMatches_whenFourPlayersRegistered() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(
                registrationWithPlayer(), registrationWithPlayer(),
                registrationWithPlayer(), registrationWithPlayer()
        ));
        startTournamentService.startTournament(1L);
        verify(saveMatchPort, times(2)).saveMatch(any(Match.class));
    }

    @Test
    void startTournament_shouldSetTournamentInProgress_whenStarted() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(
                registrationWithPlayer(), registrationWithPlayer()
        ));
        startTournamentService.startTournament(1L);
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(saveTournamentPort, times(1)).saveTournament(tournament);
    }

    @Test
    void startTournament_shouldCreateByeMatch_whenOddNumberOfPlayers() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(
                registrationWithPlayer(), registrationWithPlayer(), registrationWithPlayer()
        ));
        startTournamentService.startTournament(1L);
        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(2)).saveMatch(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(m -> m.getPlayer2() == null));
    }

    private Registration registrationWithPlayer() {
        Registration reg = new Registration();
        reg.setPlayer(new Player());
        return reg;
    }
}