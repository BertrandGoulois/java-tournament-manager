package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.in.GetMatchUseCase;
import com.tournament.tournament_manager.domain.port.in.RecordMatchResultUseCase;
import com.tournament.tournament_manager.domain.port.out.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.PublishMatchEventPort;
import com.tournament.tournament_manager.domain.port.out.SaveMatchPort;
import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.MatchResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation des cas d'utilisation liés aux matchs.
 *
 * <p>Dépend uniquement de ports (interfaces) - aucune dépendance directe
 * vers JPA ou Kafka. Les détails techniques sont délégués aux adapters.
 */
@Service
@Transactional(readOnly = true)
public class MatchService implements RecordMatchResultUseCase, GetMatchUseCase {

    private final LoadMatchPort loadMatchPort;
    private final SaveMatchPort saveMatchPort;
    private final PublishMatchEventPort publishMatchEventPort;

    public MatchService(LoadMatchPort loadMatchPort,
                        SaveMatchPort saveMatchPort,
                        PublishMatchEventPort publishMatchEventPort) {
        this.loadMatchPort = loadMatchPort;
        this.saveMatchPort = saveMatchPort;
        this.publishMatchEventPort = publishMatchEventPort;
    }

    /**
     * Enregistre le résultat d'un match et publie l'événement de fin de match.
     *
     * <p>Le vainqueur doit obligatoirement être l'un des deux joueurs du match.
     *
     * @param matchId identifiant du match
     * @param request contient l'identifiant du vainqueur
     * @return la représentation du match mis à jour
     * @throws MatchNotFoundException si le match n'existe pas
     * @throws InvalidException       si le match est déjà terminé
     * @throws InvalidException       si le vainqueur déclaré n'est pas un joueur du match
     */
    @Override
    @Transactional
    public MatchResponse recordMatchResult(Long matchId, RecordMatchResultRequest request) {
        Match match = loadMatchPort.loadMatch(matchId);

        if (match.getStatus() == MatchStatus.FINISHED) {
            throw new InvalidException("Match already finished");
        }

        if (!request.winnerId().equals(match.getPlayer1().getId()) &&
                !request.winnerId().equals(match.getPlayer2().getId())) {
            throw new InvalidException("Winner is not a player of this match");
        }

        match.setWinner(match.getPlayer1().getId().equals(request.winnerId())
                ? match.getPlayer1()
                : match.getPlayer2());
        match.setStatus(MatchStatus.FINISHED);

        Match saved = saveMatchPort.saveMatch(match);
        publishMatchEventPort.publishMatchFinished(new MatchFinishedEvent(saved.getId()));

        return toResponse(saved);
    }

    /**
     * Retourne un match par son identifiant.
     *
     * @param id identifiant du match
     * @return la représentation du match
     * @throws MatchNotFoundException si le match n'existe pas
     */
    @Override
    public MatchResponse getMatchById(Long id) {
        return toResponse(loadMatchPort.loadMatch(id));
    }

    /**
     * Convertit une entité {@link Match} en DTO de réponse.
     * {@code player2} et {@code winner} peuvent être null (bye).
     *
     * @param match l'entité à convertir
     * @return le DTO correspondant
     */
    private MatchResponse toResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getRound(),
                match.getStatus(),
                match.getPlayedAt(),
                match.getTournament().getId(),
                match.getPlayer1().getId(),
                match.getPlayer2() != null ? match.getPlayer2().getId() : null,
                match.getWinner() != null ? match.getWinner().getId() : null
        );
    }
}