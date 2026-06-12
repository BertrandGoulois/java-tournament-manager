package com.tournament.tournament_manager.service.bracket;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchesByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.dto.response.BracketResponse;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Spring Championship");
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        Player player1 = new Player();
        player1.setId(1L);
        Player player2 = new Player();
        player2.setId(2L);

        Match match1 = new Match();
        match1.setId(1L);
        match1.setRound(4);
        match1.setStatus(MatchStatus.FINISHED);
        match1.setPlayer1(player1);
        match1.setPlayer2(player2);
        match1.setWinner(player1);

        Match match2 = new Match();
        match2.setId(2L);
        match2.setRound(2);
        match2.setStatus(MatchStatus.PENDING);
        match2.setPlayer1(player1);
        match2.setPlayer2(player2);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(match1, match2));

        BracketResponse bracket = bracketQueryService.getBracket(1L);

        assertEquals(1L, bracket.tournamentId());
        assertEquals("Spring Championship", bracket.tournamentName());
        assertEquals(2, bracket.rounds().size());
        assertEquals(4, bracket.rounds().get(0).round());
        assertEquals(2, bracket.rounds().get(1).round());
    }

    @Test
    void getBracket_shouldReturnEmptyRounds_whenNoMatches() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Spring Championship");
        tournament.setStatus(TournamentStatus.OPEN);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of());

        BracketResponse bracket = bracketQueryService.getBracket(1L);

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
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Spring Championship");
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        Player player1 = new Player();
        player1.setId(1L);

        Match byeMatch = new Match();
        byeMatch.setId(1L);
        byeMatch.setRound(4);
        byeMatch.setStatus(MatchStatus.FINISHED);
        byeMatch.setPlayer1(player1);
        byeMatch.setPlayer2(null);
        byeMatch.setWinner(player1);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(byeMatch));

        BracketResponse bracket = bracketQueryService.getBracket(1L);

        assertNull(bracket.rounds().get(0).matches().get(0).player2Id());
        assertEquals(1L, bracket.rounds().get(0).matches().get(0).winnerId());
    }
}