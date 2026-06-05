package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.in.tournament.CreateTournamentUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.DeleteTournamentUseCase;
import com.tournament.tournament_manager.domain.port.in.tournament.GetTournamentUseCase;
import com.tournament.tournament_manager.domain.port.out.tournament.*;
import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.TournamentResponse;
import com.tournament.tournament_manager.exception.InvalidTournamentException;
import com.tournament.tournament_manager.exception.TournamentAlreadyExistsException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implémentation des cas d'utilisation liés aux tournois.
 *
 * <p>Dépend uniquement de ports (interfaces) — aucune dépendance directe vers JPA.
 * Les détails techniques sont délégués aux adapters.
 */
@Service
@Transactional(readOnly = true)
public class TournamentService implements CreateTournamentUseCase, GetTournamentUseCase, DeleteTournamentUseCase {

    private final LoadTournamentPort loadTournamentPort;
    private final SaveTournamentPort saveTournamentPort;
    private final ExistsTournamentPort existsTournamentPort;
    private final LoadAllTournamentsPort loadAllTournamentsPort;
    private final SoftDeleteTournamentPort softDeleteTournamentPort;

    public TournamentService(LoadTournamentPort loadTournamentPort,
                             SaveTournamentPort saveTournamentPort,
                             ExistsTournamentPort existsTournamentPort,
                             LoadAllTournamentsPort loadAllTournamentsPort,
                             SoftDeleteTournamentPort softDeleteTournamentPort) {
        this.loadTournamentPort = loadTournamentPort;
        this.saveTournamentPort = saveTournamentPort;
        this.existsTournamentPort = existsTournamentPort;
        this.loadAllTournamentsPort = loadAllTournamentsPort;
        this.softDeleteTournamentPort = softDeleteTournamentPort;
    }

    @Override
    @Transactional
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

    @Override
    public TournamentResponse getTournamentById(Long id) {
        return toResponse(loadTournamentPort.loadTournament(id));
    }

    /**
     * Désactive un tournoi sans le supprimer physiquement de la base.
     *
     * @param id identifiant du tournoi
     * @throws TournamentNotFoundException si le tournoi n'existe pas
     */
    @Override
    @Transactional
    public void deleteTournament(Long id) {
        Tournament tournament = loadTournamentPort.loadTournament(id);
        tournament.setDeleted(true);
        tournament.setDeletedAt(LocalDateTime.now());
        softDeleteTournamentPort.softDeleteTournament(tournament);
    }

    /**
     * Retourne une page de tournois.
     *
     * @param pageable paramètres de pagination (page, taille, tri)
     * @return une page de tournois
     */
    @Override
    public Page<TournamentResponse> getAllTournaments(Pageable pageable) {
        return loadAllTournamentsPort.loadAllTournaments(pageable)
                .map(this::toResponse);
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