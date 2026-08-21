package com.tournament.tournament_manager.application.bracket;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchesByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.domain.model.Bracket;
import com.tournament.tournament_manager.exception.domain.TournamentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;

@ExtendWith(MockitoExtension.class)
class BracketQueryServiceTest {

    @Mock
    private LoadTournamentPort loadTournamentPort;
    @Mock
    private LoadMatchesByTournamentPort loadMatchesByTournamentPort;

    @InjectMocks
    private BracketQueryService bracketQueryService;

    @Test
    void getBracket_shouldReturnBracketWithRounds() {
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Spring Championship"), TournamentStatus.IN_PROGRESS, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);

        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);

        Match match1 = Match.reconstitute(1L, 4, 0, null, MatchStatus.FINISHED, null, null, null, player1, player2, player1);

        Match match2 = Match.reconstitute(2L, 2, 0, null, MatchStatus.PENDING, null, null, null, player1, player2, null);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(match1, match2));

        Bracket bracket = bracketQueryService.getBracket(1L);

        assertEquals(1L, bracket.tournamentId());
        assertEquals("Spring Championship", bracket.tournamentName());
        assertEquals(2, bracket.rounds().size());
        assertEquals(4, bracket.rounds().get(0).round());
        assertEquals(2, bracket.rounds().get(1).round());
    }

    @Test
    void getBracket_shouldReturnEmptyRounds_whenNoMatches() {
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Spring Championship"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of());

        Bracket bracket = bracketQueryService.getBracket(1L);

        assertEquals(0, bracket.rounds().size());
    }

    @Test
    void getBracket_shouldThrow_whenTournamentNotFound() {
        when(loadTournamentPort.loadTournament(99L)).thenThrow(new TournamentNotFoundException(99L));
        assertThrows(TournamentNotFoundException.class,
                () -> bracketQueryService.getBracket(99L));
    }

    @Test
    void getBracket_shouldHandleByeMatch() {
        Tournament tournament = Tournament.reconstitute(1L, new TournamentName("Spring Championship"), TournamentStatus.IN_PROGRESS, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);

        Player player1 = new Player();
        player1.setId(1L);

        Match byeMatch = Match.reconstitute(1L, 4, 0, null, MatchStatus.FINISHED, null, null, null, player1, null, player1);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(byeMatch));

        Bracket bracket = bracketQueryService.getBracket(1L);

        assertNull(bracket.rounds().get(0).matches().get(0).getPlayer2());
        assertEquals(1L, bracket.rounds().get(0).matches().get(0).getWinner().getId());
    }
}