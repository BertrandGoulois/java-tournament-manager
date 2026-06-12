package com.tournament.tournament_manager.service.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.out.tournament.ExistsTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.exception.InvalidTournamentException;
import com.tournament.tournament_manager.exception.TournamentAlreadyExistsException;
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
class CreateTournamentServiceTest {

    @Mock
    private SaveTournamentPort saveTournamentPort;
    @Mock
    private ExistsTournamentPort existsTournamentPort;

    @InjectMocks
    private CreateTournamentService createTournamentService;

    @Test
    void createTournament_shouldThrow_whenNameAlreadyExists() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(true);
        assertThrows(TournamentAlreadyExistsException.class,
                () -> createTournamentService.createTournament(new CreateTournamentRequest("Test", 4)));
    }

    @Test
    void createTournament_shouldThrow_whenMaxPlayersNotPowerOfTwo() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        assertThrows(InvalidTournamentException.class,
                () -> createTournamentService.createTournament(new CreateTournamentRequest("Test", 3)));
    }

    @Test
    void createTournament_shouldReturnResponse_whenValid() {
        Tournament saved = new Tournament();
        saved.setName("Test");
        saved.setMaxPlayers(4);

        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        when(saveTournamentPort.saveTournament(any())).thenReturn(saved);

        var response = createTournamentService.createTournament(new CreateTournamentRequest("Test", 4));
        assertEquals("Test", response.name());
    }

    @Test
    void createTournament_shouldThrow_whenMaxPlayersIsZeroOrNegative() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        assertThrows(InvalidTournamentException.class,
                () -> createTournamentService.createTournament(new CreateTournamentRequest("Test", 0)));
    }
}