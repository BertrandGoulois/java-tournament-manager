package com.tournament.tournament_manager.application.registration;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.port.out.registration.LoadRegistrationPort;
import com.tournament.tournament_manager.dto.response.registration.RegistrationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

        Pageable pageable = PageRequest.of(0, 20);

        when(loadRegistrationPort.loadByTournamentId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(registration), pageable, 1));

        Page<RegistrationResponse> responses =
                getRegistrationsService.getTournamentRegistrations(1L, pageable);

        assertEquals(1, responses.getContent().size());
        assertEquals(1L, responses.getContent().getFirst().id());
    }

    @Test
    void getTournamentRegistrations_shouldReturnEmptyList_whenNoRegistrations() {
        Pageable pageable = PageRequest.of(0, 20);
        when(loadRegistrationPort.loadByTournamentId(1L, pageable)).thenReturn(Page.empty(pageable));
        Page<RegistrationResponse> responses = getRegistrationsService.getTournamentRegistrations(1L, pageable);
        assertTrue(responses.isEmpty());
    }
}