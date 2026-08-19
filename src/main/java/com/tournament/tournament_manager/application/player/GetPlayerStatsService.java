package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.EloHistory;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.PlayerStats;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerStatsUseCase;
import com.tournament.tournament_manager.domain.port.out.player.CountMatchesByPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadEloHistoryPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cas d'utilisation : consultation des statistiques d'un joueur. Retourne un objet de
 * domaine pur ({@link PlayerStats}) — voir la Javadoc de {@code GetPlayerService}.
 */
@Service
@Transactional(readOnly = true)
public class GetPlayerStatsService implements GetPlayerStatsUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final CountMatchesByPlayerPort countMatchesByPlayerPort;
    private final LoadEloHistoryPort loadEloHistoryPort;

    public GetPlayerStatsService(LoadPlayerPort loadPlayerPort,
                                 CountMatchesByPlayerPort countMatchesByPlayerPort,
                                 LoadEloHistoryPort loadEloHistoryPort) {
        this.loadPlayerPort = loadPlayerPort;
        this.countMatchesByPlayerPort = countMatchesByPlayerPort;
        this.loadEloHistoryPort = loadEloHistoryPort;
    }

    @Override
    @Cacheable(value = "playerStats", key = "#id")
    public PlayerStats getPlayerStats(Long id) {
        Player player = loadPlayerPort.loadPlayer(id);

        long matchesPlayed = countMatchesByPlayerPort.countByPlayer(id);
        long wins = countMatchesByPlayerPort.countWinsByPlayer(id);
        long losses = matchesPlayed - wins;
        double winRate = matchesPlayed == 0 ? 0 : (double) wins / matchesPlayed * 100;

        List<EloHistory> history = loadEloHistoryPort.loadByPlayerIdOrderByDateDesc(id);

        return new PlayerStats(
                player,
                (int) matchesPlayed,
                (int) wins,
                (int) losses,
                Math.round(winRate * 100.0) / 100.0,
                history
        );
    }
}
