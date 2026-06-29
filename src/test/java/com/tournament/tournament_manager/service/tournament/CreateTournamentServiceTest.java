package com.tournament.tournament_manager.service.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.tournament.ExistsTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.dto.request.tournament.CreateTournamentRequest;
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
                () -> createTournamentService.createTournament(new CreateTournamentRequest("Test", 4, TournamentFormat.SINGLE_ELIMINATION, null, null)));
    }

    @Test
    void createTournament_shouldThrow_whenMaxPlayersNotPowerOfTwo() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        assertThrows(InvalidTournamentException.class,
                () -> createTournamentService.createTournament(new CreateTournamentRequest("Test", 3, TournamentFormat.SINGLE_ELIMINATION, null, null)));
    }

    @Test
    void createTournament_shouldReturnResponse_whenValid() {
        Tournament saved = new Tournament();
        saved.setName("Test");
        saved.setMaxPlayers(4);

        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        when(saveTournamentPort.saveTournament(any())).thenReturn(saved);

        var response = createTournamentService.createTournament(new CreateTournamentRequest("Test", 4, TournamentFormat.SINGLE_ELIMINATION, null, null));
        assertEquals("Test", response.name());
    }

    @Test
    void createTournament_shouldThrow_whenMaxPlayersIsZeroOrNegative() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        assertThrows(InvalidTournamentException.class,
                () -> createTournamentService.createTournament(new CreateTournamentRequest("Test", 0, TournamentFormat.SINGLE_ELIMINATION, null, null)));
    }

    @Test
    void createTournament_shouldThrow_whenGroupsButNumberOfGroupsMissing() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        assertThrows(InvalidTournamentException.class,
                () -> createTournamentService.createTournament(
                        new CreateTournamentRequest("Test", 8, TournamentFormat.GROUPS_THEN_KNOCKOUT, null, 2)));
    }

    @Test
    void createTournament_shouldThrow_whenMaxPlayersNotDivisibleByNumberOfGroups() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        assertThrows(InvalidTournamentException.class,
                () -> createTournamentService.createTournament(
                        new CreateTournamentRequest("Test", 9, TournamentFormat.GROUPS_THEN_KNOCKOUT, 2, 1)));
    }

    @Test
    void createTournament_shouldThrow_whenQualifiersPerGroupTooHigh() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        assertThrows(InvalidTournamentException.class,
                () -> createTournamentService.createTournament(
                        new CreateTournamentRequest("Test", 8, TournamentFormat.GROUPS_THEN_KNOCKOUT, 2, 4)));
    }

    @Test
    void createTournament_shouldThrow_whenTotalQualifiersNotPowerOfTwo() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        // 3 groupes * 1 qualifié = 3 qualifiés, pas une puissance de 2
        assertThrows(InvalidTournamentException.class,
                () -> createTournamentService.createTournament(
                        new CreateTournamentRequest("Test", 9, TournamentFormat.GROUPS_THEN_KNOCKOUT, 3, 1)));
    }

    @Test
    void createTournament_shouldSucceed_whenGroupsConfigurationValid() {
        Tournament saved = new Tournament();
        saved.setName("Test");
        saved.setMaxPlayers(8);
        saved.setFormat(TournamentFormat.GROUPS_THEN_KNOCKOUT);
        saved.setNumberOfGroups(2);
        saved.setQualifiersPerGroup(2);

        when(existsTournamentPort.existsByName("Test")).thenReturn(false);
        when(saveTournamentPort.saveTournament(any())).thenReturn(saved);

        // 2 groupes de 4 joueurs, 2 qualifiés/groupe = 4 qualifiés au total (puissance de 2)
        var response = createTournamentService.createTournament(
                new CreateTournamentRequest("Test", 8, TournamentFormat.GROUPS_THEN_KNOCKOUT, 2, 2));

        assertEquals(2, response.numberOfGroups());
        assertEquals(2, response.qualifiersPerGroup());
    }
}