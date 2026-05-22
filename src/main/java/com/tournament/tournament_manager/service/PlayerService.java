package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.in.player.CreatePlayerUseCase;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerStatsUseCase;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerUseCase;
import com.tournament.tournament_manager.domain.port.out.player.*;
import com.tournament.tournament_manager.dto.request.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.EloHistoryResponse;
import com.tournament.tournament_manager.dto.response.PlayerResponse;
import com.tournament.tournament_manager.dto.response.PlayerStatsResponse;
import com.tournament.tournament_manager.exception.PlayerAlreadyExistsException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation des cas d'utilisation liés aux joueurs.
 *
 * <p>Dépend uniquement de ports (interfaces) — aucune dépendance directe vers JPA.
 * Les détails techniques sont délégués aux adapters.
 */
@Service
@Transactional(readOnly = true)
public class PlayerService implements CreatePlayerUseCase, GetPlayerUseCase, GetPlayerStatsUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final SavePlayerPort savePlayerPort;
    private final ExistsPlayerPort existsPlayerPort;
    private final LoadAllPlayersPort loadAllPlayersPort;
    private final CountMatchesByPlayerPort countMatchesByPlayerPort;
    private final LoadEloHistoryPort loadEloHistoryPort;

    public PlayerService(LoadPlayerPort loadPlayerPort,
                         SavePlayerPort savePlayerPort,
                         ExistsPlayerPort existsPlayerPort,
                         LoadAllPlayersPort loadAllPlayersPort,
                         CountMatchesByPlayerPort countMatchesByPlayerPort,
                         LoadEloHistoryPort loadEloHistoryPort) {
        this.loadPlayerPort = loadPlayerPort;
        this.savePlayerPort = savePlayerPort;
        this.existsPlayerPort = existsPlayerPort;
        this.loadAllPlayersPort = loadAllPlayersPort;
        this.countMatchesByPlayerPort = countMatchesByPlayerPort;
        this.loadEloHistoryPort = loadEloHistoryPort;
    }

    /**
     * Crée un nouveau joueur avec un classement ELO par défaut.
     *
     * @param request contient le username et l'email du joueur
     * @return la représentation du joueur créé
     * @throws PlayerAlreadyExistsException si le username ou l'email est déjà utilisé
     */
    @Override
    @Transactional
    public PlayerResponse createPlayer(CreatePlayerRequest request) {
        if (existsPlayerPort.existsByUsername(request.username())) {
            throw new PlayerAlreadyExistsException("username", request.username());
        }
        if (existsPlayerPort.existsByEmail(request.email())) {
            throw new PlayerAlreadyExistsException("email", request.email());
        }
        Player player = new Player();
        player.setUsername(request.username());
        player.setEmail(request.email());
        return toResponse(savePlayerPort.savePlayer(player));
    }

    /**
     * Retourne un joueur par son identifiant.
     *
     * @param id identifiant du joueur
     * @return la représentation du joueur
     */
    @Override
    public PlayerResponse getPlayerById(Long id) {
        return toResponse(loadPlayerPort.loadPlayer(id));
    }

    /**
     * Retourne une page de joueurs.
     *
     * @param pageable paramètres de pagination (page, taille, tri)
     * @return une page de joueurs
     */
    @Override
    public Page<PlayerResponse> getAllPlayers(Pageable pageable) {
        return loadAllPlayersPort.loadAllPlayers(pageable)
                .map(this::toResponse);
    }

    /**
     * Retourne les statistiques complètes d'un joueur : matchs joués,
     * victoires, défaites, win rate et historique ELO.
     *
     * <p>Le résultat est mis en cache Redis ({@code playerStats}) par identifiant joueur.
     * Le win rate est exprimé en pourcentage (0.0 à 100.0), arrondi à deux décimales.
     * Les matchs de bye sont comptabilisés dans {@code matchesPlayed} et {@code wins}.
     *
     * @param id identifiant du joueur
     * @return les statistiques du joueur
     */
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

    /**
     * Convertit une entité {@link Player} en DTO de réponse.
     *
     * @param player l'entité à convertir
     * @return le DTO correspondant
     */
    private PlayerResponse toResponse(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getUsername(),
                player.getEmail(),
                player.getEloRating().value(),
                player.getCreatedAt()
        );
    }
}