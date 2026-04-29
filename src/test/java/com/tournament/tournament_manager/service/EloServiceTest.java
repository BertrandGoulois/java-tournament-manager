package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.valueobjects.EloRating;
import com.tournament.tournament_manager.repository.EloHistoryRepository;
import com.tournament.tournament_manager.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EloServiceTest {

    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private EloHistoryRepository eloHistoryRepository;

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

        verify(playerRepository, times(1)).saveAll(anyList());
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

        verify(eloHistoryRepository, times(2)).save(any());
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
}