package com.tournament.tournament_manager.infrastructure.input.messaging;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.port.in.elo.UpdateEloUseCase;
import com.tournament.tournament_manager.domain.port.out.elo.ExistsEloHistoryPort;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;

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
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, player1, player2, null);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(existsEloHistoryPort.existsByMatchId(1L)).thenReturn(false);

        eloListener.onMatchFinished(new MatchFinishedEvent(1L, 0, 0));

        verify(updateEloUseCase, times(1)).updateElo(match);
    }

    @Test
    void onMatchFinished_bye_shouldSkipEloUpdate() {
        Player player1 = new Player();
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, player1, null, null);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);

        eloListener.onMatchFinished(new MatchFinishedEvent(1L, 0, 0));

        verifyNoInteractions(updateEloUseCase);
    }

    @Test
    void onMatchFinished_alreadyProcessed_shouldSkipEloUpdate() {
        Player player1 = new Player();
        Player player2 = new Player();
        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, player1, player2, null);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(existsEloHistoryPort.existsByMatchId(1L)).thenReturn(true);

        eloListener.onMatchFinished(new MatchFinishedEvent(1L, 0, 0));

        verifyNoInteractions(updateEloUseCase);
    }

    @Test
    void onMatchFinished_shouldThrow_whenMatchNotFound() {
        when(loadMatchPort.loadMatch(99L)).thenThrow(new MatchNotFoundException(99L));

        assertThrows(MatchNotFoundException.class,
                () -> eloListener.onMatchFinished(new MatchFinishedEvent(99L, 0, 0)));
    }

    /**
     * Point 2.1 de la revue. Le rattrapage de la violation de contrainte
     * {@code UNIQUE(match_id, player_id)} vit désormais ici, et non plus dans
     * {@code EloService} : à l'intérieur d'une méthode {@code @Transactional}, la transaction
     * est déjà marquée rollback-only au moment où l'exception est levée, et l'avaler ne fait
     * que la muer en {@code UnexpectedRollbackException} au commit — hors de portée du catch.
     * Depuis le listener, la transaction est terminée et l'événement peut être acquitté.
     */
    @Test
    void onMatchFinished_shouldAcknowledge_whenConcurrentExecutionWonTheRace() {
        Player player1 = new Player();
        Player player2 = new Player();
        Match match = Match.reconstitute(1L, 0, 0, null, MatchStatus.PENDING, null, null, null,
                player1, player2, player1);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(existsEloHistoryPort.existsByMatchId(1L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(updateEloUseCase).updateElo(match);

        assertDoesNotThrow(() -> eloListener.onMatchFinished(new MatchFinishedEvent(1L, 0, 0)));
    }

    /**
     * Même scénario, mais quand la violation ne remonte qu'au commit : Spring l'enveloppe
     * alors dans une {@code UnexpectedRollbackException}. C'est la forme sous laquelle
     * l'exception arrivait déjà avant le correctif — et qui repartait en retry puis en DLT.
     */
    @Test
    void onMatchFinished_shouldAcknowledge_whenRollbackSurfacesAtCommit() {
        Player player1 = new Player();
        Player player2 = new Player();
        Match match = Match.reconstitute(1L, 0, 0, null, MatchStatus.PENDING, null, null, null,
                player1, player2, player1);

        when(loadMatchPort.loadMatch(1L)).thenReturn(match);
        when(existsEloHistoryPort.existsByMatchId(1L)).thenReturn(false);
        doThrow(new UnexpectedRollbackException("transaction marquée rollback-only"))
                .when(updateEloUseCase).updateElo(match);

        assertDoesNotThrow(() -> eloListener.onMatchFinished(new MatchFinishedEvent(1L, 0, 0)));
    }
}