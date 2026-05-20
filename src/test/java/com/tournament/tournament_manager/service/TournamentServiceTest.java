package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.out.ExistsTournamentPort;
import com.tournament.tournament_manager.domain.port.out.LoadAllTournamentsPort;
import com.tournament.tournament_manager.domain.port.out.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.SaveTournamentPort;
import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.TournamentResponse;
import com.tournament.tournament_manager.exception.InvalidTournamentException;
import com.tournament.tournament_manager.exception.TournamentAlreadyExistsException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
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
class TournamentServiceTest {

    @Mock
    private LoadTournamentPort loadTournamentPort;
    @Mock
    private SaveTournamentPort saveTournamentPort;
    @Mock
    private ExistsTournamentPort existsTournamentPort;
    @Mock
    private LoadAllTournamentsPort loadAllTournamentsPort;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void createTournament_shouldThrow_whenNameAlreadyExists() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(true);
        assertThrows(TournamentAlreadyExistsException.class,
                () -> tournamentService.createTournament(new CreateTournamentRequest("Test", 4)));
    }

    @Test
    void createTournament_shouldThrow_whenMaxPlayersNotPowerOfTwo() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        assertThrows(InvalidTournamentException.class,
                () -> tournamentService.createTournament(new CreateTournamentRequest("Test", 3)));
    }

    @Test
    void createTournament_shouldReturnResponse_whenValid() {
        Tournament saved = new Tournament();
        saved.setName("Test");
        saved.setMaxPlayers(4);

        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        when(saveTournamentPort.saveTournament(any())).thenReturn(saved);

        var response = tournamentService.createTournament(new CreateTournamentRequest("Test", 4));
        assertEquals("Test", response.name());
    }

    @Test
    void getTournamentById_shouldThrow_whenNotFound() {
        when(loadTournamentPort.loadTournament(1L)).thenThrow(new TournamentNotFoundException(1L));
        assertThrows(TournamentNotFoundException.class, () -> tournamentService.getTournamentById(1L));
    }

    @Test
    void getTournamentById_shouldReturnTournament_whenFound() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Test");
        tournament.setMaxPlayers(4);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);

        var response = tournamentService.getTournamentById(1L);
        assertEquals("Test", response.name());
    }

    @Test
    void getAllTournaments_shouldReturnList() {
        Tournament tournament = new Tournament();
        tournament.setName("Test");
        tournament.setMaxPlayers(4);

        Page<Tournament> page = new PageImpl<>(List.of(tournament));
        when(loadAllTournamentsPort.loadAllTournaments(any(Pageable.class))).thenReturn(page);

        Page<TournamentResponse> responses = tournamentService.getAllTournaments(Pageable.unpaged());

        assertEquals(1, responses.getTotalElements());
        assertEquals("Test", responses.getContent().get(0).name());
    }

    @Test
    void createTournament_shouldThrow_whenMaxPlayersIsZeroOrNegative() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        assertThrows(InvalidTournamentException.class,
                () -> tournamentService.createTournament(new CreateTournamentRequest("Test", 0)));
    }
}