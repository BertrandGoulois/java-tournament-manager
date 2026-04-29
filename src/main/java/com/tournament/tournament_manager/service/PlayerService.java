package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.dto.request.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.EloHistoryResponse;
import com.tournament.tournament_manager.dto.response.PlayerResponse;
import com.tournament.tournament_manager.dto.response.PlayerStatsResponse;
import com.tournament.tournament_manager.exception.PlayerAlreadyExistsException;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import com.tournament.tournament_manager.repository.EloHistoryRepository;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final EloHistoryRepository eloHistoryRepository;

    public PlayerService(PlayerRepository playerRepository, MatchRepository matchRepository, EloHistoryRepository eloHistoryRepository) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.eloHistoryRepository = eloHistoryRepository;
    }

    @Transactional()
    public PlayerResponse createPlayer(CreatePlayerRequest request) {
        if (playerRepository.existsByUsername(request.username())) {
            throw new PlayerAlreadyExistsException("username", request.username());
        }
        if (playerRepository.existsByEmail(request.email())) {
            throw new PlayerAlreadyExistsException("email", request.email());
        }
        Player player = new Player();
        player.setUsername(request.username());
        player.setEmail(request.email());
        return toResponse(playerRepository.save(player));
    }

    public PlayerResponse getPlayerById(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
        return toResponse(player);
    }

    public List<PlayerResponse> getAllPlayers() {
        return playerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PlayerStatsResponse getPlayerStats(Long id) {
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));

        long matchesPlayed = matchRepository.countByPlayer1IdOrPlayer2Id(id, id);
        long wins = matchRepository.countByWinnerId(id);
        long losses = matchesPlayed - wins;
        double winRate = matchesPlayed == 0 ? 0 : (double) wins / matchesPlayed * 100;

        List<EloHistoryResponse> history = eloHistoryRepository.findByPlayerIdOrderByCreatedAtDesc(id)
                .stream()
                .map(e -> new EloHistoryResponse(e.getEloChange(), e.getEloAfter(), e.getCreatedAt(), e.getMatch().getId()))
                .collect(Collectors.toList());

        return new PlayerStatsResponse(
                player.getId(),
                player.getUsername(),
                player.getEloRating().value(),
                (int) matchesPlayed,
                (int) wins,
                (int) losses,
                winRate,
                history
        );
    }

    private PlayerResponse toResponse(Player player) {
        return new PlayerResponse(player.getId(), player.getUsername(), player.getEmail(), player.getEloRating().value(), player.getCreatedAt());
    }
}
