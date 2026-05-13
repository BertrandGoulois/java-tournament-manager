package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.dto.request.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.RegistrationResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import com.tournament.tournament_manager.repository.PlayerRepository;
import com.tournament.tournament_manager.repository.RegistrationRepository;
import com.tournament.tournament_manager.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registerPlayer_shouldThrow_whenPlayerNotFound() {
        when(playerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(PlayerNotFoundException.class,
                () -> registrationService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentNotFound() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(new Player()));
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(TournamentNotFoundException.class,
                () -> registrationService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentNotOpen() {
        Player player = new Player();
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThrows(InvalidException.class,
                () -> registrationService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenPlayerAlreadyRegistered() {
        Player player = new Player();
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(true);

        assertThrows(InvalidException.class,
                () -> registrationService.registerPlayer(new CreateRegistrationRequest(1L, 1L)));
    }

    @Test
    void registerPlayer_shouldThrow_whenTournamentFull() {
        Player player = new Player();
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByTournamentId(1L)).thenReturn(4L);

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

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player));
        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(false);
        when(registrationRepository.countByTournamentId(1L)).thenReturn(0L);
        when(registrationRepository.save(any())).thenReturn(registration);

        RegistrationResponse response = registrationService.registerPlayer(new CreateRegistrationRequest(1L, 1L));

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

        when(registrationRepository.findByTournamentId(1L)).thenReturn(List.of(registration));

        List<RegistrationResponse> responses = registrationService.getTournamentRegistrations(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());
    }
}