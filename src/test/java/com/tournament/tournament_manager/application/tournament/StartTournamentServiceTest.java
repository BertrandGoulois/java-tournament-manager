package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentStartStrategy;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.NotFoundException;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;

@ExtendWith(MockitoExtension.class)
class StartTournamentServiceTest {

    private MeterRegistry meterRegistry = new SimpleMeterRegistry();
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
                List.of(singleEliminationStrategy),
                meterRegistry
        );
    }

    @Test
    void startTournament_shouldThrowException_whenTournamentNotOpen() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.IN_PROGRESS, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        assertThrows(InvalidException.class, () -> startTournamentService.startTournament(1L));
    }

    @Test
    void startTournament_shouldThrowException_whenLessThanTwoPlayers() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
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
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 4, null, false, null);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(
                registrationWithPlayer(), registrationWithPlayer()
        ));

        startTournamentService.startTournament(1L);

        verify(singleEliminationStrategy, times(1)).generateInitialMatches(eq(tournament), any());
    }

    @Test
    void startTournament_shouldSetTournamentInProgress_whenStarted() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 4, null, false, null);
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
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.ROUND_ROBIN, null, null, 4, null, false, null); // pas de strategy ROUND_ROBIN enregistrée dans ce test
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(
                registrationWithPlayer(), registrationWithPlayer()
        ));
        assertThrows(InvalidException.class, () -> startTournamentService.startTournament(1L));
    }

    @Test
    void startTournament_shouldThrowException_whenGroupsThenKnockoutPlayerCountNotDivisibleByGroups() {
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.GROUPS_THEN_KNOCKOUT, 2, null, 10, null, false, null);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(
                registrationWithPlayer(), registrationWithPlayer(), registrationWithPlayer(),
                registrationWithPlayer(), registrationWithPlayer(), registrationWithPlayer(),
                registrationWithPlayer()
        ));

        assertThrows(InvalidException.class, () -> startTournamentService.startTournament(1L));
        verifyNoInteractions(saveTournamentPort);
    }

    private Registration registrationWithPlayer() {
        Registration reg = new Registration();
        reg.setPlayer(new Player());
        return reg;
    }
}