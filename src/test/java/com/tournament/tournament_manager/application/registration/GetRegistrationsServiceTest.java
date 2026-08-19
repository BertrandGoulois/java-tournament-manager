package com.tournament.tournament_manager.application.registration;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        PageRequest pageRequest = PageRequest.of(0, 20);

        when(loadRegistrationPort.loadByTournamentId(eq(1L), any()))
                .thenReturn(PageResult.of(List.of(registration), 0, 20, 1));

        PageResult<Registration> responses =
                getRegistrationsService.getTournamentRegistrations(1L, pageRequest);

        assertEquals(1, responses.content().size());
        assertEquals(1L, responses.content().get(0).getId());
    }

    @Test
    void getTournamentRegistrations_shouldReturnEmptyList_whenNoRegistrations() {
        PageRequest pageRequest = PageRequest.of(0, 20);
        when(loadRegistrationPort.loadByTournamentId(1L, pageRequest))
                .thenReturn(PageResult.of(List.of(), 0, 20, 0));
        PageResult<Registration> responses =
                getRegistrationsService.getTournamentRegistrations(1L, pageRequest);
        assertTrue(responses.content().isEmpty());
    }
}
