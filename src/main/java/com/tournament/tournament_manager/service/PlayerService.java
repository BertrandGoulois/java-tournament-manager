package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.dto.request.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.PlayerResponse;
import com.tournament.tournament_manager.exception.PlayerAlreadyExistsException;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import com.tournament.tournament_manager.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerService {
    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

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

    public PlayerResponse getPlayerById(Long id){
        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new PlayerNotFoundException(id));
        return toResponse(player);
    }

    public List<PlayerResponse> getAllPlayers(){
        return playerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PlayerResponse getPlayerStats(Long id){
        return getPlayerById(id);
    }

    private PlayerResponse toResponse(Player player){
        return new PlayerResponse(player.getId(), player.getUsername(), player.getEmail(), player.getEloRating(), player.getCreatedAt());
    }
}
