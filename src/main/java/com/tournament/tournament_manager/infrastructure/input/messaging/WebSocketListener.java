package com.tournament.tournament_manager.infrastructure.input.messaging;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.tournament.tournament_manager.config.kafka.KafkaConfig.MATCH_FINISHED_TOPIC;

/**
 * Consomme les événements {@link MatchFinishedEvent} depuis le topic Kafka
 * {@code match-finished} et notifie les clients WebSocket connectés.
 *
 * <p>Publie l'événement sur le topic STOMP {@code /topic/matches},
 * auquel les clients peuvent s'abonner pour recevoir les mises à jour en temps réel.
 */
@Slf4j
@Component
public class WebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Retransmet l'événement de fin de match aux clients WebSocket abonnés.
     *
     * @param event l'événement contenant l'identifiant du match terminé
     */
    @KafkaListener(topics = MATCH_FINISHED_TOPIC, groupId = KafkaConfig.WEBSOCKET_GROUP)
    public void onMatchFinished(MatchFinishedEvent event) {
        log.info("Notification WebSocket envoyée [matchId={}]", event.matchId());
        messagingTemplate.convertAndSend("/topic/matches", event);
    }
}