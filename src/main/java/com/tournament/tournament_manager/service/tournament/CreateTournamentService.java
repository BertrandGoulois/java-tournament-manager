package com.tournament.tournament_manager.service.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.in.tournament.CreateTournamentUseCase;
import com.tournament.tournament_manager.domain.port.out.tournament.ExistsTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.TournamentResponse;
import com.tournament.tournament_manager.exception.InvalidTournamentException;
import com.tournament.tournament_manager.exception.TournamentAlreadyExistsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : création d'un tournoi.
 */
@Service
@Transactional
public class CreateTournamentService implements CreateTournamentUseCase {

    private final SaveTournamentPort saveTournamentPort;
    private final ExistsTournamentPort existsTournamentPort;

    public CreateTournamentService(SaveTournamentPort saveTournamentPort,
                                   ExistsTournamentPort existsTournamentPort) {
        this.saveTournamentPort = saveTournamentPort;
        this.existsTournamentPort = existsTournamentPort;
    }

    @Override
    public TournamentResponse createTournament(CreateTournamentRequest request) {
        if (existsTournamentPort.existsByName(request.name())) {
            throw new TournamentAlreadyExistsException(request.name());
        }
        if (!isPowerOfTwo(request.maxPlayers())) {
            throw new InvalidTournamentException(request.maxPlayers());
        }
        Tournament tournament = new Tournament();
        tournament.setName(request.name());
        tournament.setMaxPlayers(request.maxPlayers());
        return toResponse(saveTournamentPort.saveTournament(tournament));
    }

    private TournamentResponse toResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getStatus(),
                tournament.getMaxPlayers(),
                tournament.getCreatedAt()
        );
    }

    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}