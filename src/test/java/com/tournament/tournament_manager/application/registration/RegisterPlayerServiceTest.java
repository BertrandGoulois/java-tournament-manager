package com.tournament.tournament_manager.application.registration;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.domain.port.out.registration.CountRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.ExistsRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.SaveRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.domain.model.RegisterPlayerCommand;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;

@ExtendWith(MockitoExtension.class)
class RegisterPlayerServiceTest {

    @Mock
    private LoadPlayerPort loadPlayerPort;
    @Mock
    private LoadTournamentPort loadTournamentPort;
    @Mock
    private SaveRegistrationPort saveRegistrationPort;
    @Mock
    private ExistsRegistrationPort existsRegistrationPort;
    @Mock
    private CountRegistrationPort countRegistrationPort;

    @InjectMocks
    private RegisterPlayerService registerPlayerService;

    @Test
    void registerPlayer_shouldThrow_whenPlayerNotFound() {
        when(loadPlayerPort.loadPlayer(1L)).thenThrow(new PlayerNotFoundException(1L));
        assertThrows(PlayerNotFoundException.class,
                () -> registerPlayerService.registerPlayer(new RegisterPlayerCommand(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentNotFound() {
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(new Player());
        when(loadTournamentPort.loadTournament(1L)).thenThrow(new TournamentNotFoundException(1L));
        assertThrows(TournamentNotFoundException.class,
                () -> registerPlayerService.registerPlayer(new RegisterPlayerCommand(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentNotOpen() {
        Player player = new Player();
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.IN_PROGRESS, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        assertThrows(InvalidException.class,
                () -> registerPlayerService.registerPlayer(new RegisterPlayerCommand(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenPlayerAlreadyRegistered() {
        Player player = new Player();
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 4, null, false, null);
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(existsRegistrationPort.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(true);
        assertThrows(InvalidException.class,
                () -> registerPlayerService.registerPlayer(new RegisterPlayerCommand(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentFull() {
        Player player = new Player();
        Tournament tournament = Tournament.reconstitute(null, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 4, null, false, null);
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(existsRegistrationPort.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(false);
        when(countRegistrationPort.countByTournamentId(1L)).thenReturn(4L);
        assertThrows(InvalidException.class,
                () -> registerPlayerService.registerPlayer(new RegisterPlayerCommand(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldReturnRegistration_whenValid() {
        Player player = new Player();
        player.setId(1L);
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 4, null, false, null);
        Registration registration = new Registration();
        registration.setId(1L);
        registration.setPlayer(player);
        registration.setTournament(tournament);
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(existsRegistrationPort.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(false);
        when(countRegistrationPort.countByTournamentId(1L)).thenReturn(0L);
        when(saveRegistrationPort.saveRegistration(any())).thenReturn(registration);
        Registration result = registerPlayerService.registerPlayer(
                new RegisterPlayerCommand(1L, 1L));
        assertEquals(1L, result.getId());
    }

    @Test
    void registerPlayer_shouldReturnCorrectPlayerId() {
        Player player = new Player();
        player.setId(1L);
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 8, null, false, null);
        Registration registration = new Registration();
        registration.setId(1L);
        registration.setPlayer(player);
        registration.setTournament(tournament);

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(existsRegistrationPort.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(false);
        when(countRegistrationPort.countByTournamentId(1L)).thenReturn(0L);
        when(saveRegistrationPort.saveRegistration(any())).thenReturn(registration);

        Registration result = registerPlayerService.registerPlayer(
                new RegisterPlayerCommand(1L, 1L));

        assertEquals(1L, result.getPlayer().getId());
        assertEquals(1L, result.getTournament().getId());
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentStatusIsInProgress() {
        Player player = new Player();
        player.setId(1L);
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.IN_PROGRESS, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);

        assertThrows(InvalidException.class,
                () -> registerPlayerService.registerPlayer(new RegisterPlayerCommand(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentStatusIsFinished() {
        Player player = new Player();
        player.setId(1L);
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.FINISHED, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);

        assertThrows(InvalidException.class,
                () -> registerPlayerService.registerPlayer(new RegisterPlayerCommand(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldSaveRegistrationWithCorrectPlayerAndTournament() {
        Player player = new Player();
        player.setId(1L);
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 8, null, false, null);

        Registration saved = new Registration();
        saved.setId(1L);
        saved.setPlayer(player);
        saved.setTournament(tournament);

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(existsRegistrationPort.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(false);
        when(countRegistrationPort.countByTournamentId(1L)).thenReturn(0L);

        ArgumentCaptor<Registration> captor = ArgumentCaptor.forClass(
                Registration.class);
        when(saveRegistrationPort.saveRegistration(captor.capture())).thenReturn(saved);

        registerPlayerService.registerPlayer(new RegisterPlayerCommand(1L, 1L));

        assertEquals(player, captor.getValue().getPlayer());
        assertEquals(tournament, captor.getValue().getTournament());
    }
}