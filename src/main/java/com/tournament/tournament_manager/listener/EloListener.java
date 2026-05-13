package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.service.EloService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


import static com.tournament.tournament_manager.config.kafka.KafkaConfig.MATCH_FINISHED_TOPIC;

/**
 * Consomme les événements {@link MatchFinishedEvent} depuis le topic Kafka
 * {@code match-finished} et déclenche la mise à jour des classements ELO.
 *
 * <p>Les matchs de bye (sans {@code player2}) sont ignorés : aucun calcul ELO
 * n'est effectué pour une qualification automatique.
 */
@Component
public class EloListener {

    private final EloService eloService;
    private final MatchRepository matchRepository;

    public EloListener(EloService eloService, MatchRepository matchRepository) {
        this.eloService = eloService;
        this.matchRepository = matchRepository;
    }

    /**
     * Récupère le match correspondant à l'événement et délègue
     * le calcul ELO à {@link EloService}.
     * Ignoré si le match est un bye ({@code player2 == null}).
     *
     * @param event l'événement contenant l'identifiant du match terminé
     * @throws MatchNotFoundException si le match n'existe pas
     */
    @KafkaListener(topics = MATCH_FINISHED_TOPIC, groupId = "elo-group")
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = matchRepository.findById(event.matchId())
                .orElseThrow(() -> new MatchNotFoundException(event.matchId()));
        if (match.getPlayer2() == null) return;
        eloService.updateElo(match);
    }
}