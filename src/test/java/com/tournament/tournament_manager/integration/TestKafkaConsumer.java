package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
public class TestKafkaConsumer {

    private CountDownLatch latch = new CountDownLatch(1);
    private MatchFinishedEvent receivedEvent;

    @KafkaListener(topics = KafkaConfig.MATCH_FINISHED_TOPIC, groupId = "test-group")
    public void consume(MatchFinishedEvent event) {
        this.receivedEvent = event;
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public MatchFinishedEvent getReceivedEvent() {
        return receivedEvent;
    }
}