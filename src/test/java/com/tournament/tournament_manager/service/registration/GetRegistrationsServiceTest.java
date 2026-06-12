package com.tournament.tournament_manager.service.registration;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.dto.response.RegistrationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRegistrationsServiceTest {

    @Mock
    private LoadRegistrationPort loadRegistrationPort;

    @InjectMocks
    private GetRegistrationsService getRegistrationsService;

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

        List<RegistrationResponse> responses = getRegistrationsService.getTournamentRegistrations(1L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).id());
    }

    @Test
    void getTournamentRegistrations_shouldReturnEmptyList_whenNoRegistrations() {
        when(loadRegistrationPort.loadByTournamentId(1L)).thenReturn(List.of());
        List<RegistrationResponse> responses = getRegistrationsService.getTournamentRegistrations(1L);
        assertTrue(responses.isEmpty());
    }
}