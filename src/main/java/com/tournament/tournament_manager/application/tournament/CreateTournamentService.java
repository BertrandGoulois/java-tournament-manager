package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.CreateTournamentCommand;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.port.in.tournament.CreateTournamentUseCase;
import com.tournament.tournament_manager.domain.port.out.tournament.ExistsTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.exception.domain.InvalidTournamentException;
import com.tournament.tournament_manager.exception.domain.TournamentAlreadyExistsException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : création d'un tournoi. Retourne un objet de domaine pur — voir la
 * Javadoc de {@code GetPlayerService}.
 *
 * <p>La valeur par défaut du format ({@code SINGLE_ELIMINATION} si non précisé) est
 * désormais résolue par l'adaptateur d'entrée (contrôleur REST, handler JSON-RPC) au
 * moment de construire {@link CreateTournamentCommand} — pas ici. Ce service reçoit
 * toujours un format déjà résolu.
 */
@Service
@Transactional
public class CreateTournamentService implements CreateTournamentUseCase {

    private final SaveTournamentPort saveTournamentPort;
    private final ExistsTournamentPort existsTournamentPort;
    private final Counter tournamentCreatedCounter;

    public CreateTournamentService(SaveTournamentPort saveTournamentPort,
                                   ExistsTournamentPort existsTournamentPort,
                                   MeterRegistry meterRegistry) {
        this.saveTournamentPort = saveTournamentPort;
        this.existsTournamentPort = existsTournamentPort;
        this.tournamentCreatedCounter = Counter.builder("tournament.created")
                .description("Nombre de tournois créés")
                .register(meterRegistry);
    }

    @Override
    public Tournament createTournament(CreateTournamentCommand command) {
        if (existsTournamentPort.existsByName(command.name())) {
            throw new TournamentAlreadyExistsException(command.name());
        }

        TournamentFormat format = command.format();

        if (format == TournamentFormat.SINGLE_ELIMINATION && !isPowerOfTwo(command.maxPlayers())) {
            throw new InvalidTournamentException(command.maxPlayers());
        }

        Integer numberOfGroups = null;
        Integer qualifiersPerGroup = null;

        if (format == TournamentFormat.GROUPS_THEN_KNOCKOUT) {
            numberOfGroups = validateAndResolveNumberOfGroups(command);
            qualifiersPerGroup = validateAndResolveQualifiersPerGroup(command, numberOfGroups);
        }

        Tournament tournament = Tournament.create(
                new TournamentName(command.name()), command.maxPlayers(), format,
                numberOfGroups, qualifiersPerGroup);
        Tournament saved = saveTournamentPort.saveTournament(tournament);

        tournamentCreatedCounter.increment();
        return saved;
    }

    private int validateAndResolveNumberOfGroups(CreateTournamentCommand command) {
        Integer numberOfGroups = command.numberOfGroups();
        if (numberOfGroups == null || numberOfGroups < 2) {
            throw new InvalidTournamentException(
                    "numberOfGroups doit être renseigné et >= 2 pour le format GROUPS_THEN_KNOCKOUT");
        }
        if (command.maxPlayers() % numberOfGroups != 0) {
            throw new InvalidTournamentException(
                    "maxPlayers (" + command.maxPlayers() + ") doit être divisible par numberOfGroups (" + numberOfGroups + ")");
        }
        return numberOfGroups;
    }

    private int validateAndResolveQualifiersPerGroup(CreateTournamentCommand command, int numberOfGroups) {
        Integer qualifiersPerGroup = command.qualifiersPerGroup();
        int groupSize = command.maxPlayers() / numberOfGroups;

        if (qualifiersPerGroup == null || qualifiersPerGroup < 1) {
            throw new InvalidTournamentException(
                    "qualifiersPerGroup doit être renseigné et >= 1 pour le format GROUPS_THEN_KNOCKOUT");
        }
        if (qualifiersPerGroup >= groupSize) {
            throw new InvalidTournamentException(
                    "qualifiersPerGroup (" + qualifiersPerGroup + ") doit être strictement inférieur à la taille d'un groupe (" + groupSize + ")");
        }
        int totalQualifiers = numberOfGroups * qualifiersPerGroup;
        if (!isPowerOfTwo(totalQualifiers)) {
            throw new InvalidTournamentException(
                    "Le nombre total de qualifiés (" + totalQualifiers + ") doit être une puissance de 2 pour le bracket final");
        }
        return qualifiersPerGroup;
    }

    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}
