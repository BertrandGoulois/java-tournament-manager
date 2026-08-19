package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadAllTournamentsPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        var result = getTournamentService.getTournamentById(1L);
        assertEquals("Test", result.getName());
    }

    @Test
    void getAllTournaments_shouldReturnList() {
        Tournament tournament = new Tournament();
        tournament.setName("Test");
        tournament.setMaxPlayers(4);

        PageResult<Tournament> page = PageResult.of(List.of(tournament), 0, 20, 1);
        when(loadAllTournamentsPort.loadAllTournaments(any())).thenReturn(page);

        PageResult<Tournament> responses = getTournamentService.getAllTournaments(
                com.tournament.tournament_manager.domain.model.PageRequest.of(0, 20));

        assertEquals(1, responses.totalElements());
        assertEquals("Test", responses.content().get(0).getName());
    }
}