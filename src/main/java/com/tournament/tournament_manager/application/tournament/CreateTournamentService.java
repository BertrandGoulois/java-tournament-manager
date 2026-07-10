package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.in.tournament.CreateTournamentUseCase;
import com.tournament.tournament_manager.domain.port.out.tournament.ExistsTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.dto.request.tournament.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.tournament.TournamentResponse;
import com.tournament.tournament_manager.exception.domain.InvalidTournamentException;
import com.tournament.tournament_manager.exception.domain.TournamentAlreadyExistsException;
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

        TournamentFormat format = request.format();

        if (format == TournamentFormat.SINGLE_ELIMINATION && !isPowerOfTwo(request.maxPlayers())) {
            throw new InvalidTournamentException(request.maxPlayers());
        }

        Integer numberOfGroups = null;
        Integer qualifiersPerGroup = null;

        if (format == TournamentFormat.GROUPS_THEN_KNOCKOUT) {
            numberOfGroups = validateAndResolveNumberOfGroups(request);
            qualifiersPerGroup = validateAndResolveQualifiersPerGroup(request, numberOfGroups);
        }

        Tournament tournament = new Tournament();
        tournament.setName(request.name());
        tournament.setMaxPlayers(request.maxPlayers());
        tournament.setFormat(format);
        tournament.setNumberOfGroups(numberOfGroups);
        tournament.setQualifiersPerGroup(qualifiersPerGroup);
        return toResponse(saveTournamentPort.saveTournament(tournament));
    }

    /**
     * Valide et retourne le nombre de groupes pour un tournoi {@code GROUPS_THEN_KNOCKOUT}.
     *
     * @throws InvalidTournamentException si le nombre de groupes est absent, &lt; 2,
     *                                    ou si l'effectif n'est pas divisible également entre les groupes
     */
    private int validateAndResolveNumberOfGroups(CreateTournamentRequest request) {
        Integer numberOfGroups = request.numberOfGroups();
        if (numberOfGroups == null || numberOfGroups < 2) {
            throw new InvalidTournamentException(
                    "numberOfGroups doit être renseigné et >= 2 pour le format GROUPS_THEN_KNOCKOUT");
        }
        if (request.maxPlayers() % numberOfGroups != 0) {
            throw new InvalidTournamentException(
                    "maxPlayers (" + request.maxPlayers() + ") doit être divisible par numberOfGroups (" + numberOfGroups + ")");
        }
        return numberOfGroups;
    }

    /**
     * Valide et retourne le nombre de qualifiés par groupe pour un tournoi {@code GROUPS_THEN_KNOCKOUT}.
     *
     * @throws InvalidTournamentException si la valeur est absente, supérieure ou égale à la taille
     *                                    d'un groupe, ou si le total des qualifiés n'est pas une
     *                                    puissance de 2 (nécessaire pour le bracket qui suit)
     */
    private int validateAndResolveQualifiersPerGroup(CreateTournamentRequest request, int numberOfGroups) {
        Integer qualifiersPerGroup = request.qualifiersPerGroup();
        int groupSize = request.maxPlayers() / numberOfGroups;

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

    private TournamentResponse toResponse(Tournament tournament) {
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

    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}