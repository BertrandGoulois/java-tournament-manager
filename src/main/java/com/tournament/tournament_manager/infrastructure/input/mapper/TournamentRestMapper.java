package com.tournament.tournament_manager.infrastructure.input.mapper;

import com.tournament.tournament_manager.domain.model.Bracket;
import com.tournament.tournament_manager.domain.model.BracketRound;
import com.tournament.tournament_manager.domain.model.CreateTournamentCommand;
import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.StandingEntry;
import com.tournament.tournament_manager.domain.model.Standings;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.dto.request.tournament.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.tournament.BracketMatchResponse;
import com.tournament.tournament_manager.dto.response.tournament.BracketResponse;
import com.tournament.tournament_manager.dto.response.tournament.BracketRoundResponse;
import com.tournament.tournament_manager.dto.response.tournament.StandingEntryResponse;
import com.tournament.tournament_manager.dto.response.tournament.StandingsResponse;
import com.tournament.tournament_manager.dto.response.tournament.TournamentResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convertit entre le domaine pur ({@link Tournament}, {@link Bracket}, {@link Standings})
 * et les DTO REST. Voir la Javadoc de {@code PlayerRestMapper}.
 */
@Component
public class TournamentRestMapper {

    public TournamentResponse toResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getStatus(),
                tournament.getFormat(),
                tournament.getMaxPlayers(),
                tournament.getNumberOfGroups(),
                tournament.getQualifiersPerGroup(),
                tournament.getCreatedAt()
        );
    }

    /**
     * La valeur par défaut du format ({@code SINGLE_ELIMINATION} si non précisé) est résolue
     * ici, à la frontière — voir la Javadoc de {@code CreateTournamentService} (point 35 de
     * la revue : cette résolution ne doit plus être un accesseur de record surchargé).
     */
    public CreateTournamentCommand toCommand(CreateTournamentRequest request) {
        TournamentFormat format = request.format() != null ? request.format() : TournamentFormat.SINGLE_ELIMINATION;
        return new CreateTournamentCommand(
                request.name(),
                request.maxPlayers(),
                format,
                request.numberOfGroups(),
                request.qualifiersPerGroup()
        );
    }

    public BracketResponse toResponse(Bracket bracket) {
        List<BracketRoundResponse> rounds = bracket.rounds().stream()
                .map(this::toResponse)
                .toList();
        return new BracketResponse(bracket.tournamentId(), bracket.tournamentName(), bracket.status(), rounds);
    }

    private BracketRoundResponse toResponse(BracketRound round) {
        List<BracketMatchResponse> matches = round.matches().stream()
                .map(this::toBracketMatchResponse)
                .toList();
        return new BracketRoundResponse(round.round(), matches);
    }

    private BracketMatchResponse toBracketMatchResponse(Match match) {
        return new BracketMatchResponse(
                match.getId(),
                match.getPosition(),
                match.getPlayer1().getId(),
                match.getPlayer2() != null ? match.getPlayer2().getId() : null,
                match.getWinner() != null ? match.getWinner().getId() : null,
                match.getStatus()
        );
    }

    public StandingsResponse toResponse(Standings standings) {
        List<StandingEntryResponse> entries = standings.standings().stream()
                .map(this::toEntryResponse)
                .toList();
        return new StandingsResponse(standings.tournamentId(), standings.tournamentName(), entries);
    }

    private StandingEntryResponse toEntryResponse(StandingEntry entry) {
        return new StandingEntryResponse(
                entry.player().getId(),
                entry.player().getUsername(),
                entry.matchesPlayed(),
                entry.wins(),
                entry.losses(),
                entry.points()
        );
    }
}
