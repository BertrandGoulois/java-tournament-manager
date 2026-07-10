package com.tournament.tournament_manager.application.elo;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.valueobjects.EloRating;
import com.tournament.tournament_manager.domain.port.out.elo.SaveAllPlayersPort;
import com.tournament.tournament_manager.domain.port.out.elo.SaveEloHistoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EloServiceTest {

    @Mock
    private SaveAllPlayersPort saveAllPlayersPort;
    @Mock
    private SaveEloHistoryPort saveEloHistoryPort;

    @InjectMocks
    private EloService eloService;

    @Test
    void updateElo_shouldIncreaseWinnerElo() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1000));

        Match match = new Match();
        match.setPlayer1(winner);
        match.setPlayer2(loser);
        match.setWinner(winner);

        eloService.updateElo(match);

        assert winner.getEloRating().value() > 1000;
        assert loser.getEloRating().value() < 1000;
    }

    @Test
    void updateElo_shouldSaveBothPlayers() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1000));

        Match match = new Match();
        match.setPlayer1(winner);
        match.setPlayer2(loser);
        match.setWinner(winner);

        eloService.updateElo(match);

        verify(saveAllPlayersPort, times(1)).saveAllPlayers(anyList());
    }

    @Test
    void updateElo_shouldCreateTwoEloHistoryEntries() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1000));

        Match match = new Match();
        match.setPlayer1(winner);
        match.setPlayer2(loser);
        match.setWinner(winner);

        eloService.updateElo(match);

        verify(saveEloHistoryPort, times(2)).saveEloHistory(any());
    }

    @Test
    void updateElo_shouldGiveMoreEloWhenBeatingStrongerOpponent() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player strongLoser = new Player();
        strongLoser.setEloRating(new EloRating(1400));

        Match match = new Match();
        match.setPlayer1(winner);
        match.setPlayer2(strongLoser);
        match.setWinner(winner);

        eloService.updateElo(match);

        assert winner.getEloRating().value() > 1016;
    }

    @Test
    void updateElo_shouldUpdateElo_whenPlayer2IsWinner() {
        Player player1 = new Player();
        player1.setEloRating(new EloRating(1000));
        Player player2 = new Player();
        player2.setEloRating(new EloRating(1000));

        Match match = new Match();
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setWinner(player2);

        eloService.updateElo(match);

        assert player2.getEloRating().value() > 1000;
        assert player1.getEloRating().value() < 1000;
    }
}