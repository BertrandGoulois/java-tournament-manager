package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.in.GetRegistrationsUseCase;
import com.tournament.tournament_manager.domain.port.in.RegisterPlayerUseCase;
import com.tournament.tournament_manager.domain.port.out.CountRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.ExistsRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.LoadPlayerPort;
import com.tournament.tournament_manager.domain.port.out.LoadRegistrationPort;
import com.tournament.tournament_manager.domain.port.out.LoadTournamentPort;
import com.tournament.tournament_manager.domain.port.out.SaveRegistrationPort;
import com.tournament.tournament_manager.dto.request.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.RegistrationResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation des cas d'utilisation liés aux inscriptions.
 *
 * <p>Dépend uniquement de ports (interfaces) — aucune dépendance directe vers JPA.
 * Les détails techniques sont délégués aux adapters.
 */
@Service
@Transactional(readOnly = true)
public class RegistrationService implements RegisterPlayerUseCase, GetRegistrationsUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final LoadTournamentPort loadTournamentPort;
    private final SaveRegistrationPort saveRegistrationPort;
    private final ExistsRegistrationPort existsRegistrationPort;
    private final CountRegistrationPort countRegistrationPort;
    private final LoadRegistrationPort loadRegistrationPort;

    public RegistrationService(LoadPlayerPort loadPlayerPort,
                               LoadTournamentPort loadTournamentPort,
                               SaveRegistrationPort saveRegistrationPort,
                               ExistsRegistrationPort existsRegistrationPort,
                               CountRegistrationPort countRegistrationPort,
                               LoadRegistrationPort loadRegistrationPort) {
        this.loadPlayerPort = loadPlayerPort;
        this.loadTournamentPort = loadTournamentPort;
        this.saveRegistrationPort = saveRegistrationPort;
        this.existsRegistrationPort = existsRegistrationPort;
        this.countRegistrationPort = countRegistrationPort;
        this.loadRegistrationPort = loadRegistrationPort;
    }

    /**
     * Inscrit un joueur à un tournoi.
     *
     * <p>Trois conditions sont vérifiées dans l'ordre :
     * <ol>
     *   <li>Le tournoi doit être au statut {@code OPEN}</li>
     *   <li>Le joueur ne doit pas être déjà inscrit à ce tournoi</li>
     *   <li>Le nombre d'inscrits ne doit pas avoir atteint {@code maxPlayers}</li>
     * </ol>
     *
     * @param request contient l'identifiant du joueur et du tournoi
     * @return la représentation de l'inscription créée
     * @throws com.tournament.tournament_manager.exception.PlayerNotFoundException     si le joueur n'existe pas
     * @throws com.tournament.tournament_manager.exception.TournamentNotFoundException si le tournoi n'existe pas
     * @throws InvalidException si le tournoi n'est pas ouvert aux inscriptions
     * @throws InvalidException si le joueur est déjà inscrit
     * @throws InvalidException si le tournoi est complet
     */
    @Override
    @Transactional
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

    /**
     * Retourne la liste des inscriptions d'un tournoi.
     *
     * @param tournamentId identifiant du tournoi
     * @return liste des inscriptions, vide si aucun joueur inscrit
     */
    @Override
    public List<RegistrationResponse> getTournamentRegistrations(Long tournamentId) {
        return loadRegistrationPort.loadByTournamentId(tournamentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une entité {@link Registration} en DTO de réponse.
     *
     * @param registration l'entité à convertir
     * @return le DTO correspondant
     */
    private RegistrationResponse toResponse(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getPlayer().getId(),
                registration.getTournament().getId(),
                registration.getRegisteredAt()
        );
    }
}