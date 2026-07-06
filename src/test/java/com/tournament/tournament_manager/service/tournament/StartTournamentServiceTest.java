package com.tournament.tournament_manager.service.tournament;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentStartStrategy;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.NotFoundException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private TournamentStartStrategy singleEliminationStrategy;

    private StartTournamentService startTournamentService;

    @BeforeEach
    void setUp() {
        lenient().when(singleEliminationStrategy.supportedFormat())
                .thenReturn(TournamentFormat.SINGLE_ELIMINATION);
        startTournamentService = new StartTournamentService(
                loadTournamentPort,
                saveTournamentPort,
                loadRegistrationPort,
                List.of(singleEliminationStrategy)
        );
    }

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
    void startTournament_shouldDelegateToMatchingStrategy() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);
        tournament.setFormat(TournamentFormat.SINGLE_ELIMINATION);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(
                registrationWithPlayer(), registrationWithPlayer()
        ));

        startTournamentService.startTournament(1L);

        verify(singleEliminationStrategy, times(1)).generateInitialMatches(eq(tournament), any());
    }

    @Test
    void startTournament_shouldSetTournamentInProgress_whenStarted() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);
        tournament.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(
                registrationWithPlayer(), registrationWithPlayer()
        ));
        startTournamentService.startTournament(1L);
        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(saveTournamentPort, times(1)).saveTournament(tournament);
    }

    @Test
    void startTournament_shouldThrow_whenNoStrategyRegisteredForFormat() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);
        tournament.setFormat(TournamentFormat.ROUND_ROBIN); // pas de strategy ROUND_ROBIN enregistrée dans ce test
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(
                registrationWithPlayer(), registrationWithPlayer()
        ));
        assertThrows(InvalidException.class, () -> startTournamentService.startTournament(1L));
    }

    private Registration registrationWithPlayer() {
        Registration reg = new Registration();
        reg.setPlayer(new Player());
        return reg;
    }
}