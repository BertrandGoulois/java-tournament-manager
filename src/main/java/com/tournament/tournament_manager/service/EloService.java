package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.repository.EloHistoryRepository;
import com.tournament.tournament_manager.repository.PlayerRepository;
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

    @Transactional
    public void updateElo(Match match){
        Player winner = match.getWinner();
        Player loser = match.getPlayer1().equals(match.getWinner())
                ? match.getPlayer2()
                : match.getPlayer1();

        int eloWinner = winner.getEloRating();
        int eloLoser = loser.getEloRating();

        double expectedWinner = 1.0 / (1 + Math.pow(10, (eloLoser - eloWinner) / 400.0));
        double expectedLoser = 1.0 - expectedWinner;

        int K = 32;
        int newEloWinner = (int) Math.round(eloWinner + K * (1 - expectedWinner));
        int newEloLoser = (int) Math.round(eloLoser + K * (0 - expectedLoser));

        winner.setEloRating(newEloWinner);
        loser.setEloRating(newEloLoser);

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
