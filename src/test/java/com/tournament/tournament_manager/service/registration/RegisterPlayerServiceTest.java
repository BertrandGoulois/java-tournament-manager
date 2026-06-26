package com.tournament.tournament_manager.service.registration;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.domain.port.out.registration.CountRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.ExistsRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.SaveRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.dto.request.registration.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.registration.RegistrationResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
                () -> registerPlayerService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentNotFound() {
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(new Player());
        when(loadTournamentPort.loadTournament(1L)).thenThrow(new TournamentNotFoundException(1L));
        assertThrows(TournamentNotFoundException.class,
                () -> registerPlayerService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentNotOpen() {
        Player player = new Player();
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        assertThrows(InvalidException.class,
                () -> registerPlayerService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenPlayerAlreadyRegistered() {
        Player player = new Player();
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(existsRegistrationPort.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(true);
        assertThrows(InvalidException.class,
                () -> registerPlayerService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentFull() {
        Player player = new Player();
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(existsRegistrationPort.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(false);
        when(countRegistrationPort.countByTournamentId(1L)).thenReturn(4L);
        assertThrows(InvalidException.class,
                () -> registerPlayerService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldReturnRegistration_whenValid() {
        Player player = new Player();
        player.setId(1L);
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);
        Registration registration = new Registration();
        registration.setId(1L);
        registration.setPlayer(player);
        registration.setTournament(tournament);
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(existsRegistrationPort.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(false);
        when(countRegistrationPort.countByTournamentId(1L)).thenReturn(0L);
        when(saveRegistrationPort.saveRegistration(any())).thenReturn(registration);
        RegistrationResponse response = registerPlayerService.registerPlayer(
                new CreateRegistrationRequest(1L, 1L));
        assertEquals(1L, response.id());
    }
}