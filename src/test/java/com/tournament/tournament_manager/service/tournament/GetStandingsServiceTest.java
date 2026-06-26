package com.tournament.tournament_manager.service.tournament;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchesByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.dto.response.tournament.StandingEntryResponse;
import com.tournament.tournament_manager.dto.response.tournament.StandingsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStandingsServiceTest {

    @Mock
    private LoadTournamentPort loadTournamentPort;
    @Mock
    private LoadMatchesByTournamentPort loadMatchesByTournamentPort;

    @InjectMocks
    private GetStandingsService getStandingsService;

    @Test
    void getStandings_shouldRankPlayersByPoints() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Round Robin Cup");

        Player p1 = new Player();
        p1.setId(1L);
        p1.setUsername("alice");
        Player p2 = new Player();
        p2.setId(2L);
        p2.setUsername("bob");
        Player p3 = new Player();
        p3.setId(3L);
        p3.setUsername("carol");

        // alice bat bob, alice bat carol, bob bat carol
        Match m1 = finishedMatch(p1, p2, p1);
        Match m2 = finishedMatch(p1, p3, p1);
        Match m3 = finishedMatch(p2, p3, p2);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(m1, m2, m3));

        StandingsResponse standings = getStandingsService.getStandings(1L);

        assertEquals(3, standings.standings().size());

        StandingEntryResponse first = standings.standings().get(0);
        assertEquals("alice", first.username());
        assertEquals(2, first.wins());
        assertEquals(6, first.points());

        StandingEntryResponse second = standings.standings().get(1);
        assertEquals("bob", second.username());
        assertEquals(1, second.wins());
        assertEquals(1, second.losses());

        StandingEntryResponse third = standings.standings().get(2);
        assertEquals("carol", third.username());
        assertEquals(0, third.wins());
        assertEquals(2, third.losses());
    }

    @Test
    void getStandings_shouldIgnorePendingMatches() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Round Robin Cup");

        Player p1 = new Player();
        p1.setId(1L);
        p1.setUsername("alice");
        Player p2 = new Player();
        p2.setId(2L);
        p2.setUsername("bob");

        Match pending = new Match();
        pending.setPlayer1(p1);
        pending.setPlayer2(p2);
        pending.setStatus(MatchStatus.PENDING);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(pending));

        StandingsResponse standings = getStandingsService.getStandings(1L);

        assertEquals(2, standings.standings().size());
        assertTrue(standings.standings().stream().allMatch(s -> s.points() == 0));
    }

    @Test
    void getStandings_shouldIgnoreByeMatches() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setName("Round Robin Cup");

        Player p1 = new Player();
        p1.setId(1L);
        p1.setUsername("alice");

        Match bye = new Match();
        bye.setPlayer1(p1);
        bye.setPlayer2(null);
        bye.setStatus(MatchStatus.FINISHED);
        bye.setWinner(p1);

        when(loadTournamentPort.loadTournament(1L)).thenReturn(tournament);
        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(bye));

        StandingsResponse standings = getStandingsService.getStandings(1L);

        assertEquals(1, standings.standings().size());
        assertEquals(3, standings.standings().get(0).points());
    }

    private Match finishedMatch(Player player1, Player player2, Player winner) {
        Match match = new Match();
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setStatus(MatchStatus.FINISHED);
        match.setWinner(winner);
        return match;
    }
}