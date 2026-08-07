package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.TestcontainersConfiguration;
import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Le container Kafka est desormais fourni par TestcontainersConfiguration
 * (partage avec les autres tests d'integration qui declenchent la
 * publication d'un MatchFinishedEvent), plutot que declare ici en double.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private TestKafkaConsumer testKafkaConsumer;

    @Test
    void shouldReceiveMatchFinishedEvent() throws InterruptedException {
        kafkaTemplate.send(KafkaConfig.MATCH_FINISHED_TOPIC, new MatchFinishedEvent(42L, 0, 0));
        boolean received = testKafkaConsumer.getLatch().await(120, TimeUnit.SECONDS);
        assertTrue(received, "L'\u00e9v\u00e9nement Kafka n'a pas \u00e9t\u00e9 re\u00e7u dans les d\u00e9lais");
    }
}
