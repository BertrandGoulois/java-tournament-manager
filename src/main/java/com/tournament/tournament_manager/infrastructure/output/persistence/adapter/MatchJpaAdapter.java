package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.port.out.match.*;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.MatchEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.MatchMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.MatchRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter JPA implémentant les ports de chargement et sauvegarde des matchs.
 * Fait le lien entre le domaine et la couche de persistance Spring Data.
 *
 * <p>Résout les associations (tournoi, joueurs) en références JPA légères
 * ({@code repository.getReferenceById}, pas de chargement complet) avant de les passer à
 * {@link MatchMapper} — voir sa Javadoc pour l'explication de ce choix.
 */
@Component
public class MatchJpaAdapter implements LoadMatchPort, SaveMatchPort, LoadMatchByTournamentPort, LoadMatchesByTournamentPort, SaveCommentaryPort {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchMapper matchMapper;

    public MatchJpaAdapter(MatchRepository matchRepository,
                           PlayerRepository playerRepository,
                           TournamentRepository tournamentRepository,
                           MatchMapper matchMapper) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.tournamentRepository = tournamentRepository;
        this.matchMapper = matchMapper;
    }

    @Override
    public Match loadMatch(Long id) {
        MatchEntity entity = matchRepository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException(id));
        return matchMapper.toDomain(entity);
    }

    @Override
    public Match saveMatch(Match match) {
        TournamentEntity tournamentRef = tournamentRepository.getReferenceById(match.getTournament().getId());
        PlayerEntity player1Ref = playerRepository.getReferenceById(match.getPlayer1().getId());
        PlayerEntity player2Ref = referenceOrNull(match.getPlayer2());
        PlayerEntity winnerRef = referenceOrNull(match.getWinner());

        MatchEntity entity;
        if (match.getId() != null) {
            entity = matchRepository.findById(match.getId())
                    .orElseThrow(() -> new MatchNotFoundException(match.getId()));
            matchMapper.updateEntity(entity, match, tournamentRef, player1Ref, player2Ref, winnerRef);
        } else {
            entity = matchMapper.toNewEntity(match, tournamentRef, player1Ref, player2Ref, winnerRef);
        }
        MatchEntity saved = matchRepository.save(entity);
        return matchMapper.toDomain(saved);
    }

    private PlayerEntity referenceOrNull(Player player) {
        return player != null ? playerRepository.getReferenceById(player.getId()) : null;
    }

    @Override
    public List<Match> loadByTournamentIdAndRound(Long tournamentId, int round) {
        return matchRepository.findByTournamentIdAndRound(tournamentId, round).stream()
                .map(matchMapper::toDomain)
                .toList();
    }

    @Override
    public List<Match> loadByTournamentId(Long tournamentId) {
        return matchRepository.findByTournamentId(tournamentId).stream()
                .map(matchMapper::toDomain)
                .toList();
    }

    @Override
    public void saveCommentary(Long matchId, String commentary) {
        MatchEntity entity = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        entity.setCommentary(commentary);
        matchRepository.save(entity);
    }
}
