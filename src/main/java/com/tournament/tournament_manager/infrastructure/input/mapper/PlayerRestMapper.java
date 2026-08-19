package com.tournament.tournament_manager.infrastructure.input.mapper;

import com.tournament.tournament_manager.domain.model.CreatePlayerCommand;
import com.tournament.tournament_manager.domain.model.EloHistory;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.PlayerStats;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.player.EloHistoryResponse;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.dto.response.player.PlayerStatsResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convertit entre le domaine pur ({@link Player}, {@link PlayerStats}) et les DTO REST.
 * Utilisé aussi bien par les contrôleurs REST que par les handlers JSON-RPC — un choix de
 * réutilisation de code légitime à ce niveau (infrastructure), à ne pas confondre avec le
 * problème corrigé au point 22 : c'était le <b>port du domaine</b> qui imposait ce DTO à
 * tout appelant, pas un adaptateur qui choisit de le réutiliser.
 */
@Component
public class PlayerRestMapper {

    public PlayerResponse toResponse(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getUsername(),
                player.getEmail(),
                player.getEloRating().value(),
                player.getCreatedAt()
        );
    }

    public PlayerStatsResponse toStatsResponse(PlayerStats stats) {
        List<EloHistoryResponse> history = stats.eloHistory().stream()
                .map(this::toEloHistoryResponse)
                .toList();
        return new PlayerStatsResponse(
                stats.player().getId(),
                stats.player().getUsername(),
                stats.player().getEloRating().value(),
                stats.matchesPlayed(),
                stats.wins(),
                stats.losses(),
                stats.winRate(),
                history
        );
    }

    private EloHistoryResponse toEloHistoryResponse(EloHistory history) {
        return new EloHistoryResponse(
                history.getEloChange(),
                history.getEloAfter(),
                history.getCreatedAt(),
                history.getMatch().getId()
        );
    }

    public CreatePlayerCommand toCommand(CreatePlayerRequest request) {
        return new CreatePlayerCommand(request.username(), request.email());
    }
}
