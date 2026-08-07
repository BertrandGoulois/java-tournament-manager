package com.tournament.tournament_manager.infrastructure.output.persistence.mapper;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.MatchEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import org.springframework.stereotype.Component;

/**
 * Convertit entre le domaine pur {@link Match} et sa contrepartie JPA {@link MatchEntity}.
 *
 * <p>Pour la lecture ({@link #toDomain}), les associations (tournoi, joueurs) sont mappées
 * récursivement via {@link PlayerMapper}/{@link TournamentMapper}.
 *
 * <p>Pour l'écriture, {@code updateEntity}/{@code toNewEntity} attendent des <b>références</b>
 * déjà résolues ({@code TournamentEntity}, {@code PlayerEntity}) plutôt que de reconstruire ces
 * associations elles-mêmes : convertir un {@code Tournament} ou un {@code Player} du domaine en
 * entité neuve les insérerait en base une seconde fois, alors qu'on veut ici référencer une
 * ligne déjà existante. C'est à l'adapter ({@code MatchJpaAdapter}) de résoudre ces références
 * (typiquement via {@code repository.getReferenceById(id)}) et de les fournir au mapper.
 */
@Component
public class MatchMapper {

    private final PlayerMapper playerMapper;
    private final TournamentMapper tournamentMapper;

    public MatchMapper(PlayerMapper playerMapper, TournamentMapper tournamentMapper) {
        this.playerMapper = playerMapper;
        this.tournamentMapper = tournamentMapper;
    }

    public Match toDomain(MatchEntity entity) {
        if (entity == null) {
            return null;
        }
        Match match = new Match();
        match.setId(entity.getId());
        match.setRound(entity.getRound());
        match.setPosition(entity.getPosition());
        match.setGroupNumber(entity.getGroupNumber());
        match.setStatus(entity.getStatus());
        match.setPlayedAt(entity.getPlayedAt());
        match.setCommentary(entity.getCommentary());
        match.setTournament(tournamentMapper.toDomain(entity.getTournament()));
        match.setPlayer1(playerMapper.toDomain(entity.getPlayer1()));
        match.setPlayer2(playerMapper.toDomain(entity.getPlayer2()));
        match.setWinner(playerMapper.toDomain(entity.getWinner()));
        return match;
    }

    /**
     * @param tournamentRef référence résolue vers l'entité tournoi (jamais null)
     * @param player1Ref    référence résolue vers l'entité player1 (jamais null)
     * @param player2Ref    référence résolue vers l'entité player2, ou {@code null} pour un bye
     * @param winnerRef     référence résolue vers l'entité vainqueur, ou {@code null} si pas encore joué
     */
    public MatchEntity toNewEntity(Match match, TournamentEntity tournamentRef,
                                   PlayerEntity player1Ref, PlayerEntity player2Ref,
                                   PlayerEntity winnerRef) {
        MatchEntity entity = new MatchEntity();
        updateEntity(entity, match, tournamentRef, player1Ref, player2Ref, winnerRef);
        return entity;
    }

    public void updateEntity(MatchEntity entity, Match match, TournamentEntity tournamentRef,
                             PlayerEntity player1Ref, PlayerEntity player2Ref, PlayerEntity winnerRef) {
        entity.setRound(match.getRound());
        entity.setPosition(match.getPosition());
        entity.setGroupNumber(match.getGroupNumber());
        entity.setStatus(match.getStatus());
        entity.setPlayedAt(match.getPlayedAt());
        entity.setCommentary(match.getCommentary());
        entity.setTournament(tournamentRef);
        entity.setPlayer1(player1Ref);
        entity.setPlayer2(player2Ref);
        entity.setWinner(winnerRef);
    }
}
