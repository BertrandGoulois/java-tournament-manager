package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.domain.port.out.registration.CountRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.ExistsRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.SaveRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.dto.request.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.RegistrationResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

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
    @Mock
    private LoadRegistrationPort loadRegistrationPort;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registerPlayer_shouldThrow_whenPlayerNotFound() {
        when(loadPlayerPort.loadPlayer(1L)).thenThrow(new PlayerNotFoundException(1L));
        assertThrows(PlayerNotFoundException.class,
                () -> registrationService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentNotFound() {
        when(loadPlayerPort.loadPlayer(1L)).thenReturn(new Player());
        when(loadTournamentPort.loadTournament(1L)).thenThrow(new TournamentNotFoundException(1L));
        assertThrows(TournamentNotFoundException.class,
                () -> registrationService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentNotOpen() {
        Player player = new Player();
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        when(loadPlayerPort.loadPlayer(1L)).thenReturn(player);
        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);

        assertThrows(InvalidException.class,
                () -> registrationService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
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
                () -> registrationService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
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
                () -> registrationService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
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

        RegistrationResponse response = registrationService.registerPlayer(
                new CreateRegistrationRequest(1L, 1L));

        assertEquals(1L, response.id());
    }

    @Test
    void getTournamentRegistrations_shouldReturnList() {
        Player player = new Player();
        player.setId(1L);
        Tournament tournament = new Tournament();
        tournament.setId(1L);

        Registration registration = new Registration();
        registration.setId(1L);
        registration.setPlayer(player);
        registration.setTournament(tournament);

        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of(registration));

        List<RegistrationResponse> responses = registrationService.getTournamentRegistrations(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());
    }
}