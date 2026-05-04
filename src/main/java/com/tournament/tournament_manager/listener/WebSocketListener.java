package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.tournament.tournament_manager.config.kafka.KafkaConfig.MATCH_FINISHED_TOPIC;

@Component
public class WebSocketListener {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = MATCH_FINISHED_TOPIC, groupId = "websocket-group")
    public void onMatchFinished(MatchFinishedEvent event) {
        messagingTemplate.convertAndSend("/topic/matches", event);
    }
}