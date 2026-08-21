package com.tournament.tournament_manager.application.match;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.domain.model.MatchCommentary;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;

@ExtendWith(MockitoExtension.class)
class GetMatchCommentaryServiceTest {

    @Mock
    private LoadMatchPort loadMatchPort;

    @InjectMocks
    private GetMatchCommentaryService getMatchCommentaryService;

    @Test
    void getMatchCommentary_shouldReturnCommentary_whenExists() {
        Match match = Match.reconstitute(1L, 0, 0, null, MatchStatus.PENDING, null, "Super match !", null, null, null, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        MatchCommentary response = getMatchCommentaryService.getMatchCommentary(1L);
        assertEquals(1L, response.matchId());
        assertEquals("Super match !", response.commentary());
    }

    @Test
    void getMatchCommentary_shouldReturnWaitingMessage_whenCommentaryNotYetGenerated() {
        Match match = Match.reconstitute(1L, 0, 0, null, MatchStatus.PENDING, null, null, null, null, null, null);
        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        MatchCommentary response = getMatchCommentaryService.getMatchCommentary(1L);
        assertEquals("Commentaire en cours de génération...", response.commentary());
    }

    @Test
    void getMatchCommentary_shouldThrow_whenMatchNotFound() {
        when(loadMatchPort.loadMatch(99L)).thenThrow(new MatchNotFoundException(99L));
        assertThrows(MatchNotFoundException.class,
                () -> getMatchCommentaryService.getMatchCommentary(99L));
    }
}