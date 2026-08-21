package com.tournament.tournament_manager.application.match;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;

@ExtendWith(MockitoExtension.class)
class GetMatchServiceTest {

    @Mock
    private LoadMatchPort loadMatchPort;

    @InjectMocks
    private GetMatchService getMatchService;

    @Test
    void getMatchById_shouldReturnMatch_whenFound() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Match match = Match.reconstitute(1L, 0, 0, null, MatchStatus.PENDING, null, null, tournament, player1, player2, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        Match result = getMatchService.getMatchById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void getMatchById_shouldThrow_whenNotFound() {
        when(loadMatchPort.loadMatch(99L)).thenThrow(new MatchNotFoundException(99L));
        assertThrows(MatchNotFoundException.class, () -> getMatchService.getMatchById(99L));
    }

    @Test
    void getMatchById_shouldReturnMatch_withNullPlayer2AndWinner() {
        Player player1 = new Player();
        player1.setId(1L);
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Match match = Match.reconstitute(1L, 0, 0, null, MatchStatus.FINISHED, null, null, tournament, player1, null, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        Match result = getMatchService.getMatchById(1L);
        assertNull(result.getPlayer2());
        assertNull(result.getWinner());
    }

    @Test
    void getMatchById_shouldReturnPlayer2Id_whenPlayer2IsNotNull() {
        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);
        Player winner = new Player();
        winner.setId(1L);
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Match match = Match.reconstitute(1L, 0, 0, null, MatchStatus.FINISHED, null, null, tournament, player1, player2, winner);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        Match result = getMatchService.getMatchById(1L);

        assertEquals(2L, result.getPlayer2().getId());
        assertEquals(1L, result.getWinner().getId());
    }
}