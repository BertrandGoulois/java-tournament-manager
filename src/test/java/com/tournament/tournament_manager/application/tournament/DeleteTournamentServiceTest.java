package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SoftDeleteTournamentPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;

@ExtendWith(MockitoExtension.class)
class DeleteTournamentServiceTest {

    @Mock
    private LoadTournamentPort loadTournamentPort;
    @Mock
    private SoftDeleteTournamentPort softDeleteTournamentPort;

    @InjectMocks
    private DeleteTournamentService deleteTournamentService;

    @Test
    void deleteTournament_shouldSetDeletedTrue() {
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);

        deleteTournamentService.deleteTournament(1L);

        assertTrue(tournament.isDeleted());
        assertNotNull(tournament.getDeletedAt());
        verify(softDeleteTournamentPort, times(1)).softDeleteTournament(tournament);
    }
}