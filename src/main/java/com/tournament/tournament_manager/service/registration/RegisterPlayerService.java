package com.tournament.tournament_manager.service.registration;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.in.registration.RegisterPlayerUseCase;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.domain.port.out.registration.CountRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.ExistsRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.SaveRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.dto.request.registration.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.registration.RegistrationResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : inscription d'un joueur à un tournoi.
 */
@Service
@Transactional
public class RegisterPlayerService implements RegisterPlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final LoadTournamentPort loadTournamentPort;
    private final SaveRegistrationPort saveRegistrationPort;
    private final ExistsRegistrationPort existsRegistrationPort;
    private final CountRegistrationPort countRegistrationPort;

    public RegisterPlayerService(LoadPlayerPort loadPlayerPort,
                                 LoadTournamentPort loadTournamentPort,
                                 SaveRegistrationPort saveRegistrationPort,
                                 ExistsRegistrationPort existsRegistrationPort,
                                 CountRegistrationPort countRegistrationPort) {
        this.loadPlayerPort = loadPlayerPort;
        this.loadTournamentPort = loadTournamentPort;
        this.saveRegistrationPort = saveRegistrationPort;
        this.existsRegistrationPort = existsRegistrationPort;
        this.countRegistrationPort = countRegistrationPort;
    }

    @Override
    public RegistrationResponse registerPlayer(CreateRegistrationRequest request) {
        Player player = loadPlayerPort.loadPlayer(request.playerId());
        Tournament tournament = loadTournamentPort.loadTournament(request.tournamentId());

        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new InvalidException("Tournament is not open for registration");
        }
        if (existsRegistrationPort.existsByPlayerIdAndTournamentId(
                request.playerId(), request.tournamentId())) {
            throw new InvalidException("Player already registered in this tournament");
        }
        if (countRegistrationPort.countByTournamentId(request.tournamentId())
                >= tournament.getMaxPlayers()) {
            throw new InvalidException("Tournament is full");
        }

        Registration registration = new Registration();
        registration.setPlayer(player);
        registration.setTournament(tournament);
        return toResponse(saveRegistrationPort.saveRegistration(registration));
    }

    private RegistrationResponse toResponse(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getPlayer().getId(),
                registration.getTournament().getId(),
                registration.getRegisteredAt()
        );
    }
}