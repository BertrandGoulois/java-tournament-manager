package com.tournament.tournament_manager.service.match;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.dto.response.MatchResponse;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Match match = new Match();
        match.setId(1L);
        match.setStatus(MatchStatus.PENDING);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setTournament(tournament);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        MatchResponse response = getMatchService.getMatchById(1L);
        assertEquals(1L, response.id());
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
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Match match = new Match();
        match.setId(1L);
        match.setStatus(MatchStatus.FINISHED);
        match.setPlayer1(player1);
        match.setPlayer2(null);
        match.setWinner(null);
        match.setTournament(tournament);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        MatchResponse response = getMatchService.getMatchById(1L);
        assertNull(response.player2Id());
        assertNull(response.winnerId());
    }
}