package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchPort;
import com.tournament.tournament_manager.domain.port.out.strategy.TournamentProgressionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consomme les événements {@link MatchFinishedEvent} depuis le topic Kafka
 * {@code match-finished} et fait progresser le tournoi selon son format,
 * via le pattern Strategy ({@link TournamentProgressionStrategy}).
 *
 * <p>Spring injecte automatiquement toutes les implémentations de
 * {@link TournamentProgressionStrategy} disponibles dans le contexte,
 * indexées ici par {@link TournamentFormat}. Pour ajouter un nouveau format,
 * il suffit de créer une nouvelle implémentation {@code @Component} —
 * ce listener n'a pas besoin d'être modifié.
 */
@Slf4j
@Component
public class BracketListener {

    private final LoadMatchPort loadMatchPort;
    private final Map<TournamentFormat, TournamentProgressionStrategy> strategies;

    public BracketListener(LoadMatchPort loadMatchPort,
                           List<TournamentProgressionStrategy> strategyList) {
        this.loadMatchPort = loadMatchPort;
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(TournamentProgressionStrategy::supportedFormat, s -> s));
    }

    @KafkaListener(topics = KafkaConfig.MATCH_FINISHED_TOPIC, groupId = KafkaConfig.BRACKET_GROUP)
    public void onMatchFinished(MatchFinishedEvent event) {
        Match match = loadMatchPort.loadMatch(event.matchId());
        Tournament tournament = match.getTournament();

        TournamentProgressionStrategy strategy = strategies.get(tournament.getFormat());
        if (strategy == null) {
            log.error("Aucune stratégie de progression pour le format [matchId={}, format={}]",
                    event.matchId(), tournament.getFormat());
            throw new IllegalStateException("No progression strategy registered for format: " + tournament.getFormat());
        }
        log.info("Progression du tournoi [matchId={}, tournamentId={}, format={}]",
                event.matchId(), tournament.getId(), tournament.getFormat());
        strategy.onMatchFinished(match, tournament);
    }
}