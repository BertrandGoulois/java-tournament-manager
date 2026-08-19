package com.tournament.tournament_manager.application.registration;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.RegisterPlayerCommand;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.in.registration.RegisterPlayerUseCase;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.domain.port.out.registration.CountRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.ExistsRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.registration.SaveRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : inscription d'un joueur à un tournoi. Retourne un objet de domaine
 * pur — voir la Javadoc de {@code GetPlayerService}.
 */
@Slf4j
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
    public Registration registerPlayer(RegisterPlayerCommand command) {
        Player player = loadPlayerPort.loadPlayer(command.playerId());
        Tournament tournament = loadTournamentPort.loadTournament(command.tournamentId());

        if (tournament.getStatus() != TournamentStatus.OPEN) {
            log.warn("Tentative d'inscription à un tournoi non ouvert [tournamentId={}, status={}]",
                    command.tournamentId(), tournament.getStatus());
            throw new InvalidException("Tournament is not open for registration");
        }
        if (existsRegistrationPort.existsByPlayerIdAndTournamentId(
                command.playerId(), command.tournamentId())) {
            log.warn("Joueur déjà inscrit [playerId={}, tournamentId={}]",
                    command.playerId(), command.tournamentId());
            throw new InvalidException("Player already registered in this tournament");
        }
        if (countRegistrationPort.countByTournamentId(command.tournamentId())
                >= tournament.getMaxPlayers()) {
            log.warn("Tournoi complet [tournamentId={}]", command.tournamentId());
            throw new InvalidException("Tournament is full");
        }

        Registration registration = new Registration();
        registration.setPlayer(player);
        registration.setTournament(tournament);
        Registration saved = saveRegistrationPort.saveRegistration(registration);
        log.info("Joueur inscrit [playerId={}, tournamentId={}]",
                command.playerId(), command.tournamentId());
        return saved;
    }
}
