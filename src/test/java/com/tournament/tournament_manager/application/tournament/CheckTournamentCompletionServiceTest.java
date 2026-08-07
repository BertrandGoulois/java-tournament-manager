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
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        Match m1 = new Match();
        m1.setStatus(MatchStatus.FINISHED);
        Match m2 = new Match();
        m2.setStatus(MatchStatus.FINISHED);

        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(m1, m2));

        service.checkCompletion(tournament);

        assertEquals(TournamentStatus.FINISHED, tournament.getStatus());
        verify(saveTournamentPort, times(1)).saveTournament(tournament);
    }

    @Test
    void checkCompletion_shouldNotMarkFinished_whenSomeMatchesPending() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        Match m1 = new Match();
        m1.setStatus(MatchStatus.FINISHED);
        Match m2 = new Match();
        m2.setStatus(MatchStatus.PENDING);

        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(m1, m2));

        service.checkCompletion(tournament);

        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(saveTournamentPort, never()).saveTournament(any());
    }

    @Test
    void checkCompletion_shouldNotMarkFinished_whenNoMatches() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of());

        service.checkCompletion(tournament);

        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(saveTournamentPort, never()).saveTournament(any());
    }
}