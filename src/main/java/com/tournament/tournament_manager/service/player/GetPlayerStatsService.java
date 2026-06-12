package com.tournament.tournament_manager.service.player;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerStatsUseCase;
import com.tournament.tournament_manager.domain.port.out.player.CountMatchesByPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadEloHistoryPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.dto.response.EloHistoryResponse;
import com.tournament.tournament_manager.dto.response.PlayerStatsResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cas d'utilisation : consultation des statistiques d'un joueur.
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
    public PlayerStatsResponse getPlayerStats(Long id) {
        Player player = loadPlayerPort.loadPlayer(id);

        long matchesPlayed = countMatchesByPlayerPort.countByPlayer(id);
        long wins = countMatchesByPlayerPort.countWinsByPlayer(id);
        long losses = matchesPlayed - wins;
        double winRate = matchesPlayed == 0 ? 0 : (double) wins / matchesPlayed * 100;

        List<EloHistoryResponse> history = loadEloHistoryPort.loadByPlayerIdOrderByDateDesc(id)
                .stream()
                .map(e -> new EloHistoryResponse(
                        e.getEloChange(),
                        e.getEloAfter(),
                        e.getCreatedAt(),
                        e.getMatch().getId()))
                .collect(Collectors.toList());

        return new PlayerStatsResponse(
                player.getId(),
                player.getUsername(),
                player.getEloRating().value(),
                (int) matchesPlayed,
                (int) wins,
                (int) losses,
                Math.round(winRate * 100.0) / 100.0,
                history
        );
    }
}