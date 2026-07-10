package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadAllTournamentsPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.dto.response.tournament.TournamentResponse;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTournamentServiceTest {

    @Mock
    private LoadTournamentPort loadTournamentPort;
    @Mock
    private LoadAllTournamentsPort loadAllTournamentsPort;

    @InjectMocks
    private GetTournamentService getTournamentService;

    @Test
    void getTournamentById_shouldThrow_whenNotFound() {
        when(loadTournamentPort.loadTournament(1L)).thenThrow(new TournamentNotFoundException(1L));
        assertThrows(TournamentNotFoundException.class, () -> getTournamentService.getTournamentById(1L));
    }

    @Test
    void getTournamentById_shouldReturnTournament_whenFound() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Test");
        tournament.setMaxPlayers(4);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);

        var response = getTournamentService.getTournamentById(1L);
        assertEquals("Test", response.name());
    }

    @Test
    void getAllTournaments_shouldReturnList() {
        Tournament tournament = new Tournament();
        tournament.setName("Test");
        tournament.setMaxPlayers(4);

        Page<Tournament> page = new PageImpl<>(List.of(tournament));
        when(loadAllTournamentsPort.loadAllTournaments(any(Pageable.class))).thenReturn(page);

        Page<TournamentResponse> responses = getTournamentService.getAllTournaments(Pageable.unpaged());

        assertEquals(1, responses.getTotalElements());
        assertEquals("Test", responses.getContent().get(0).name());
    }
}