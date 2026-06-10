package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.dto.response.MatchCommentaryResponse;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchCommentaryServiceTest {

    @Mock
    private LoadMatchPort loadMatchPort;

    @InjectMocks
    private MatchCommentaryService matchCommentaryService;

    @Test
    void getMatchCommentary_shouldReturnCommentary_whenExists() {
        Match match = new Match();
        match.setId(1L);
        match.setCommentary("Super match !");

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        MatchCommentaryResponse response = matchCommentaryService.getMatchCommentary(1L);

        assertEquals(1L, response.matchId());
        assertEquals("Super match !", response.commentary());
    }

    @Test
    void getMatchCommentary_shouldReturnWaitingMessage_whenCommentaryNotYetGenerated() {
        Match match = new Match();
        match.setId(1L);
        match.setCommentary(null);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        MatchCommentaryResponse response = matchCommentaryService.getMatchCommentary(1L);

        assertEquals("Commentaire en cours de génération...", response.commentary());
    }

    @Test
    void getMatchCommentary_shouldThrow_whenMatchNotFound() {
        when(loadMatchPort.loadMatch(99L)).thenThrow(new MatchNotFoundException(99L));

        assertThrows(MatchNotFoundException.class,
                () -> matchCommentaryService.getMatchCommentary(99L));
    }
}