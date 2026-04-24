package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.dto.request.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.response.RegistrationResponse;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import com.tournament.tournament_manager.repository.PlayerRepository;
import com.tournament.tournament_manager.repository.RegistrationRepository;
import com.tournament.tournament_manager.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RegistrationService {

    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;

    public RegistrationService(PlayerRepository playerRepository, TournamentRepository tournamentRepository, RegistrationRepository registrationRepository) {
        this.playerRepository = playerRepository;
        this.tournamentRepository = tournamentRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    public RegistrationResponse registerPlayer(CreateRegistrationRequest request) {
        Player player = playerRepository.findById(request.playerId())
                .orElseThrow(() -> new PlayerNotFoundException(request.playerId()));
        Tournament tournament = tournamentRepository.findById(request.tournamentId())
                .orElseThrow(() -> new TournamentNotFoundException(request.tournamentId()));
        if (tournament.getStatus() != TournamentStatus.OPEN){
            throw new InvalidException("Tournament is not open for registration");
        }
        if (registrationRepository.existsByPlayerIdAndTournamentId(request.playerId(), request.tournamentId())) {
            throw new InvalidException("Player already registered in this tournament");
        }
        if (registrationRepository.countByTournamentId(request.tournamentId()) >= tournament.getMaxPlayers()) {
            throw new InvalidException("Tournament is full");
        }
        Registration registration = new Registration();
        registration.setPlayer(player);
        registration.setTournament(tournament);
        return toResponse(registrationRepository.save(registration));
    }

    public List<RegistrationResponse> getTournamentRegistrations(Long tournamentId){
        return registrationRepository.findByTournamentId(tournamentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
