package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.service.BracketService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.tournament.tournament_manager.config.kafka.KafkaConfig.MATCH_FINISHED_TOPIC;

/**
 * Consomme les événements {@link MatchFinishedEvent} depuis le topic Kafka
 * {@code match-finished} et tente de faire progresser le bracket au tour suivant.
 *
 * <p>L'avancement n'a lieu que si tous les matchs du round en cours sont terminés —
 * la logique de vérification est déléguée à {@link BracketService#advanceToNextRound}.
 */
@Component
public class BracketListener {

    private final BracketService bracketService;
    private final MatchRepository matchRepository;

    public BracketListener(BracketService bracketService, MatchRepository matchRepository){
        this.bracketService = bracketService;
        this.matchRepository = matchRepository;
    }

    /**
     * Récupère le match correspondant à l'événement et délègue
     * l'avancement du bracket à {@link BracketService}.
     *
     * @param event l'événement contenant l'identifiant du match terminé
     * @throws MatchNotFoundException si le match n'existe pas
     */
    @KafkaListener(topics = MATCH_FINISHED_TOPIC, groupId = "bracket-group")
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = matchRepository.findById(event.matchId())
                .orElseThrow(() -> new MatchNotFoundException(event.matchId()));
        bracketService.advanceToNextRound(match.getTournament(), match.getRound());
    }
}
