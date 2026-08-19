package com.tournament.tournament_manager.infrastructure.input.mapper;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.MatchCommentary;
import com.tournament.tournament_manager.domain.model.RecordMatchResultCommand;
import com.tournament.tournament_manager.dto.request.match.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.match.MatchCommentaryResponse;
import com.tournament.tournament_manager.dto.response.match.MatchResponse;
import org.springframework.stereotype.Component;

/**
 * Convertit entre le domaine pur ({@link Match}, {@link MatchCommentary}) et les DTO REST.
 * Voir la Javadoc de {@code PlayerRestMapper}.
 */
@Component
public class MatchRestMapper {

    public MatchResponse toResponse(Match match) {
        return new MatchResponse(
                match.getId(),
                match.getRound(),
                match.getPosition(),
                match.getStatus(),
                match.getPlayedAt(),
                match.getTournament().getId(),
                match.getPlayer1().getId(),
                match.getPlayer2() != null ? match.getPlayer2().getId() : null,
                match.getWinner() != null ? match.getWinner().getId() : null
        );
    }

    public MatchCommentaryResponse toResponse(MatchCommentary commentary) {
        return new MatchCommentaryResponse(commentary.matchId(), commentary.commentary());
    }

    public RecordMatchResultCommand toCommand(RecordMatchResultRequest request) {
        return new RecordMatchResultCommand(request.winnerId());
    }
}
