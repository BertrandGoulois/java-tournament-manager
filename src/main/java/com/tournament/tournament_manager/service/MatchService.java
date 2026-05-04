package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.config.KafkaConfig;
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

@Service
@Transactional(readOnly = true)
public class MatchService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.matchRepository = matchRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

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
        System.out.println(">>> Envoi Kafka pour match " + saved.getId());
        kafkaTemplate.send(KafkaConfig.MATCH_FINISHED_TOPIC, new MatchFinishedEvent(saved.getId()));
        return toResponse(saved);
    }

    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new MatchNotFoundException(id));
        return toResponse(match);
    }

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
