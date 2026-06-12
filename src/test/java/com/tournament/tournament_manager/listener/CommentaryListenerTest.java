package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.valueobjects.EloRating;
import com.tournament.tournament_manager.domain.port.out.match.GenerateCommentaryPort;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveCommentaryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentaryListenerTest {

    @Mock
    private LoadMatchPort loadMatchPort;
    @Mock
    private GenerateCommentaryPort generateCommentaryPort;
    @Mock
    private SaveCommentaryPort saveCommentaryPort;

    @InjectMocks
    private CommentaryListener commentaryListener;

    @Test
    void onMatchFinished_shouldGenerateAndSaveCommentary() {
        Player player1 = new Player();
        player1.setUsername("player1");
        player1.setEloRating(new EloRating(1200));

        Player player2 = new Player();
        player2.setUsername("player2");
        player2.setEloRating(new EloRating(1000));

        Match match = new Match();
        match.setId(1L);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setWinner(player1);
        match.setRound(4);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(generateCommentaryPort.generateCommentary(any())).thenReturn("Super match !");

        commentaryListener.onMatchFinished(new MatchFinishedEvent(1L));

        verify(generateCommentaryPort, times(1)).generateCommentary(any());
        verify(saveCommentaryPort, times(1)).saveCommentary(eq(1L), eq("Super match !"));
    }

    @Test
    void onMatchFinished_bye_shouldSkipCommentary() {
        Player player1 = new Player();
        player1.setUsername("player1");
        player1.setEloRating(new EloRating(1000));

        Match match = new Match();
        match.setId(1L);
        match.setPlayer1(player1);
        match.setPlayer2(null);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        commentaryListener.onMatchFinished(new MatchFinishedEvent(1L));

        verifyNoInteractions(generateCommentaryPort);
        verifyNoInteractions(saveCommentaryPort);
    }

    @Test
    void onMatchFinished_shouldNotThrow_whenOpenAiFails() {
        Player player1 = new Player();
        player1.setUsername("player1");
        player1.setEloRating(new EloRating(1000));

        Player player2 = new Player();
        player2.setUsername("player2");
        player2.setEloRating(new EloRating(1000));

        Match match = new Match();
        match.setId(1L);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setWinner(player1);
        match.setRound(4);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(generateCommentaryPort.generateCommentary(any())).thenThrow(new RuntimeException("OpenAI indisponible"));

        assertDoesNotThrow(() -> commentaryListener.onMatchFinished(new MatchFinishedEvent(1L)));
        verifyNoInteractions(saveCommentaryPort);
    }
}