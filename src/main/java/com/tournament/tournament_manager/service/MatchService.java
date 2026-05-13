package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.dto.request.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.MatchResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Gère l'enregistrement des résultats de matchs et la consultation des matchs.
 *
 * <p>Après enregistrement d'un résultat, publie un {@link MatchFinishedEvent}
 * sur le topic Kafka {@code match-finished}. Les listeners abonnés
 * ({@code EloListener}, {@code BracketListener}, {@code WebSocketListener})
 * réagissent ensuite de façon indépendante et asynchrone.
 */
@Service
@Transactional(readOnly = true)
public class MatchService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.matchRepository = matchRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Enregistre le résultat d'un match et publie l'événement de fin de match.
     *
     * <p>Le vainqueur doit obligatoirement être l'un des deux joueurs du match.
     * Un match de bye (sans {@code player2}) ne peut pas être soumis via cet endpoint
     * car il est résolu automatiquement à la création du bracket.
     *
     * @param matchId identifiant du match
     * @param request contient l'identifiant du vainqueur
     * @return la représentation du match mis à jour
     * @throws MatchNotFoundException si le match n'existe pas
     * @throws InvalidException       si le match est déjà terminé
     * @throws InvalidException       si le vainqueur déclaré n'est pas un joueur du match
     */
    @Transactional
    public MatchResponse recordMatchResult(Long matchId, RecordMatchResultRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));
        if (match.getStatus() != MatchStatus.PENDING) {
            throw new InvalidException("Match is already finished");
        }
        Set<Long> validPlayerIds = Set.of(match.getPlayer1().getId(), match.getPlayer2().getId());
        if (!validPlayerIds.contains(request.winnerId())) {
            throw new InvalidException("Winner must be one of the match players");
        }
        Player winner = match.getPlayer1().getId().equals(request.winnerId())
                ? match.getPlayer1()
                : match.getPlayer2();
        match.setStatus(MatchStatus.FINISHED);
        match.setWinner(winner);
        match.setPlayedAt(LocalDateTime.now());
        Match saved = matchRepository.save(match);
        kafkaTemplate.send(KafkaConfig.MATCH_FINISHED_TOPIC, new MatchFinishedEvent(saved.getId()));
        return toResponse(saved);
    }

    /**
     * Retourne un match par son identifiant.
     *
     * @param id identifiant du match
     * @return la représentation du match
     * @throws MatchNotFoundException si le match n'existe pas
     */
    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException(id));
        return toResponse(match);
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
