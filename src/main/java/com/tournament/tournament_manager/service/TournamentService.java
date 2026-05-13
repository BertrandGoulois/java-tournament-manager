package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.TournamentResponse;
import com.tournament.tournament_manager.exception.InvalidTournamentException;
import com.tournament.tournament_manager.exception.TournamentAlreadyExistsException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import com.tournament.tournament_manager.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gère la création et la consultation des tournois.
 */
@Service
@Transactional(readOnly = true)
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    /**
     * Crée un nouveau tournoi au statut {@code OPEN}.
     *
     * <p>{@code maxPlayers} doit être une puissance de 2 (4, 8, 16, 32...) —
     * contrainte nécessaire pour la génération du bracket en élimination directe.
     *
     * @param request contient le nom et le nombre maximum de joueurs
     * @return la représentation du tournoi créé
     * @throws TournamentAlreadyExistsException si le nom est déjà utilisé
     * @throws InvalidTournamentException       si {@code maxPlayers} n'est pas une puissance de 2
     */
    @Transactional
    public TournamentResponse createTournament(CreateTournamentRequest request){
        if(tournamentRepository.existsByName(request.name())){
            throw new TournamentAlreadyExistsException(request.name());
        }
        if(!isPowerOfTwo(request.maxPlayers())){
            throw new InvalidTournamentException(request.maxPlayers());
        }
        Tournament tournament = new Tournament();
        tournament.setName(request.name());
        tournament.setMaxPlayers(request.maxPlayers());
        return toResponse(tournamentRepository.save(tournament));
    }

    /**
     * Retourne un tournoi par son identifiant.
     *
     * @param id identifiant du tournoi
     * @return la représentation du tournoi
     * @throws TournamentNotFoundException si le tournoi n'existe pas
     */
    public TournamentResponse getTournamentById(Long id){
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));
        return toResponse(tournament);
    }

    /**
     * Retourne la liste de tous les tournois.
     *
     * @return liste des tournois, vide si aucun tournoi enregistré
     */
    public List<TournamentResponse> getAllTournaments(){
        return tournamentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convertit une entité {@link Tournament} en DTO de réponse.
     *
     * @param tournament l'entité à convertir
     * @return le DTO correspondant
     */
    private TournamentResponse toResponse(Tournament tournament) {
        return new TournamentResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getStatus(),
                tournament.getMaxPlayers(),
                tournament.getCreatedAt()
        );
    }

    /**
     * Vérifie qu'un entier est une puissance de 2.
     * Utilise l'astuce bit-à-bit : {@code n > 0 && (n & (n - 1)) == 0}.
     *
     * @param n la valeur à tester
     * @return {@code true} si {@code n} est une puissance de 2
     */
    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}
