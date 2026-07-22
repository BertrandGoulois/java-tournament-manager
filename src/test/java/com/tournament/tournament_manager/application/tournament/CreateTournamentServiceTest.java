package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.tournament.ExistsTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.dto.request.tournament.CreateTournamentRequest;
import com.tournament.tournament_manager.exception.domain.InvalidTournamentException;
import com.tournament.tournament_manager.exception.domain.TournamentAlreadyExistsException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private CreateTournamentService createTournamentService;

    @BeforeEach
    void setUp() {
        createTournamentService = new CreateTournamentService(saveTournamentPort, existsTournamentPort, meterRegistry);
    }

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

    @Test
    void createTournament_shouldSaveTournamentWithCorrectFields() {
        when(existsTournamentPort.existsByName("Spring Cup")).thenReturn(false);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        Tournament saved = new Tournament();
        saved.setId(1L);
        saved.setName("Spring Cup");
        saved.setMaxPlayers(8);
        saved.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        when(saveTournamentPort.saveTournament(captor.capture())).thenReturn(saved);

        createTournamentService.createTournament(
                new CreateTournamentRequest("Spring Cup", 8, TournamentFormat.SINGLE_ELIMINATION, null, null));

        Tournament captured = captor.getValue();
        assertEquals("Spring Cup", captured.getName());
        assertEquals(8, captured.getMaxPlayers());
        assertEquals(TournamentFormat.SINGLE_ELIMINATION, captured.getFormat());
    }

    @Test
    void createTournament_shouldSaveGroupsTournamentWithCorrectFields() {
        when(existsTournamentPort.existsByName("Groups Cup")).thenReturn(false);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        Tournament saved = new Tournament();
        saved.setId(1L);
        saved.setName("Groups Cup");
        saved.setMaxPlayers(8);
        saved.setFormat(TournamentFormat.GROUPS_THEN_KNOCKOUT);
        saved.setNumberOfGroups(2);
        saved.setQualifiersPerGroup(2);
        when(saveTournamentPort.saveTournament(captor.capture())).thenReturn(saved);

        createTournamentService.createTournament(
                new CreateTournamentRequest("Groups Cup", 8, TournamentFormat.GROUPS_THEN_KNOCKOUT, 2, 2));

        Tournament captured = captor.getValue();
        assertEquals("Groups Cup", captured.getName());
        assertEquals(8, captured.getMaxPlayers());
        assertEquals(TournamentFormat.GROUPS_THEN_KNOCKOUT, captured.getFormat());
        assertEquals(2, captured.getNumberOfGroups());
        assertEquals(2, captured.getQualifiersPerGroup());
    }

    @Test
    void createTournament_shouldThrow_whenNumberOfGroupsIsOne() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);

        assertThrows(InvalidTournamentException.class,
                () -> createTournamentService.createTournament(
                        new CreateTournamentRequest("Test", 8, TournamentFormat.GROUPS_THEN_KNOCKOUT, 1, 2)));
    }

    @Test
    void createTournament_shouldThrow_whenQualifiersPerGroupEqualsGroupSize() {
        when(existsTournamentPort.existsByName("Test")).thenReturn(false);

        // 8 joueurs / 2 groupes = groupSize 4, qualifiersPerGroup = 4 (>=groupSize) -> doit throw
        assertThrows(InvalidTournamentException.class,
                () -> createTournamentService.createTournament(
                        new CreateTournamentRequest("Test", 8, TournamentFormat.GROUPS_THEN_KNOCKOUT, 2, 4)));
    }
}