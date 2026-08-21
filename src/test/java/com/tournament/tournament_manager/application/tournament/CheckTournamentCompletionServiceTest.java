package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchesByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;

@ExtendWith(MockitoExtension.class)
class CheckTournamentCompletionServiceTest {

    @Mock
    private LoadMatchesByTournamentPort loadMatchesByTournamentPort;
    @Mock
    private SaveTournamentPort saveTournamentPort;

    @InjectMocks
    private CheckTournamentCompletionService service;

    @Test
    void checkCompletion_shouldMarkTournamentFinished_whenAllMatchesFinished() {
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.IN_PROGRESS, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);

        Match m1 = Match.reconstitute(null, 0, 0, null, MatchStatus.FINISHED, null, null, null, null, null, null);
        Match m2 = Match.reconstitute(null, 0, 0, null, MatchStatus.FINISHED, null, null, null, null, null, null);

        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(m1, m2));

        service.checkCompletion(tournament);

        assertEquals(TournamentStatus.FINISHED, tournament.getStatus());
        verify(saveTournamentPort, times(1)).saveTournament(tournament);
    }

    @Test
    void checkCompletion_shouldNotMarkFinished_whenSomeMatchesPending() {
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.IN_PROGRESS, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);

        Match m1 = Match.reconstitute(null, 0, 0, null, MatchStatus.FINISHED, null, null, null, null, null, null);
        Match m2 = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, null, null, null);

        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(m1, m2));

        service.checkCompletion(tournament);

        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(saveTournamentPort, never()).saveTournament(any());
    }

    @Test
    void checkCompletion_shouldNotMarkFinished_whenNoMatches() {
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.IN_PROGRESS, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);

        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of());

        service.checkCompletion(tournament);

        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(saveTournamentPort, never()).saveTournament(any());
    }
}