package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.in.elo.UpdateEloUseCase;
import com.tournament.tournament_manager.domain.port.out.elo.ExistsEloHistoryPort;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EloListenerTest {

    @Mock
    private UpdateEloUseCase updateEloUseCase;
    @Mock
    private LoadMatchPort loadMatchPort;
    @Mock
    private ExistsEloHistoryPort existsEloHistoryPort;

    @InjectMocks
    private EloListener eloListener;

    @Test
    void onMatchFinished_shouldCallUpdateElo() {
        Player player1 = new Player();
        Player player2 = new Player();
        Match match = new Match();
        match.setPlayer1(player1);
        match.setPlayer2(player2);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(existsEloHistoryPort.existsByMatchId(1L)).thenReturn(false);

        eloListener.onMatchFinished(new MatchFinishedEvent(1L));

        verify(updateEloUseCase, times(1)).updateElo(match);
    }

    @Test
    void onMatchFinished_bye_shouldSkipEloUpdate() {
        Player player1 = new Player();
        Match match = new Match();
        match.setPlayer1(player1);
        match.setPlayer2(null);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        eloListener.onMatchFinished(new MatchFinishedEvent(1L));

        verifyNoInteractions(updateEloUseCase);
    }

    @Test
    void onMatchFinished_alreadyProcessed_shouldSkipEloUpdate() {
        Player player1 = new Player();
        Player player2 = new Player();
        Match match = new Match();
        match.setPlayer1(player1);
        match.setPlayer2(player2);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(existsEloHistoryPort.existsByMatchId(1L)).thenReturn(true);

        eloListener.onMatchFinished(new MatchFinishedEvent(1L));

        verifyNoInteractions(updateEloUseCase);
    }

    @Test
    void onMatchFinished_shouldThrow_whenMatchNotFound() {
        when(loadMatchPort.loadMatch(99L)).thenThrow(new MatchNotFoundException(99L));

        assertThrows(MatchNotFoundException.class,
                () -> eloListener.onMatchFinished(new MatchFinishedEvent(99L)));
    }
}