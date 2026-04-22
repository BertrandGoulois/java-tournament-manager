package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.TournamentResponse;
import com.tournament.tournament_manager.exception.InvalidTournamentException;
import com.tournament.tournament_manager.exception.TournamentAlreadyExistsException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import com.tournament.tournament_manager.repository.TournamentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TournamentService {

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

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

    public TournamentResponse getTournamentById(Long id){
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));
        return toResponse(tournament);
    }

    public List<TournamentResponse> getAllTournaments(){
        return tournamentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
