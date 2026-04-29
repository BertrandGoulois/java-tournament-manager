package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.repository.EloHistoryRepository;
import com.tournament.tournament_manager.repository.PlayerRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EloService {

    private final PlayerRepository playerRepository;
    private final EloHistoryRepository eloHistoryRepository;

    public EloService(PlayerRepository playerRepository, EloHistoryRepository eloHistoryRepository){
        this.playerRepository = playerRepository;
        this.eloHistoryRepository = eloHistoryRepository;
    }

    @Caching(evict = {
            @CacheEvict(value = "playerStats", key = "#match.player1.id"),
            @CacheEvict(value = "playerStats", key = "#match.player2.id")
    })
    @Transactional
    public void updateElo(Match match){
        Player winner = match.getWinner();
        Player loser = match.getPlayer1().equals(match.getWinner())
                ? match.getPlayer2()
                : match.getPlayer1();

        int eloWinner = winner.getEloRating().value();
        int eloLoser = loser.getEloRating().value();

        double expectedWinner = 1.0 / (1 + Math.pow(10, (eloLoser - eloWinner) / 400.0));
        double expectedLoser = 1.0 - expectedWinner;

        int K = 32;
        int newEloWinner = (int) Math.round(eloWinner + K * (1 - expectedWinner));
        int newEloLoser = (int) Math.round(eloLoser + K * (0 - expectedLoser));

        winner.setEloRating(winner.getEloRating().add(newEloWinner - eloWinner));
        loser.setEloRating(loser.getEloRating().add(newEloLoser - eloLoser));

        playerRepository.saveAll(List.of(winner, loser));

        saveEloHistory(winner, match, newEloWinner - eloWinner, newEloWinner);
        saveEloHistory(loser, match, newEloLoser - eloLoser, newEloLoser);

    }

    private void saveEloHistory(Player player, Match match, int eloChange, int eloAfter) {
        EloHistory history = new EloHistory();
        history.setPlayer(player);
        history.setMatch(match);
        history.setEloChange(eloChange);
        history.setEloAfter(eloAfter);
        eloHistoryRepository.save(history);
    }
}
