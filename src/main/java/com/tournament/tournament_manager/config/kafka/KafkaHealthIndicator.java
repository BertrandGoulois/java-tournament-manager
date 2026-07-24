package com.tournament.tournament_manager.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Indicateur de santé custom pour Kafka.
 * Vérifie que le broker Kafka est accessible en tentant de décrire le cluster.
 */
@Slf4j
@Component("kafka")
public class KafkaHealthIndicator extends AbstractHealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            var nodes = adminClient.describeCluster().nodes().get(3, TimeUnit.SECONDS);
            if (!nodes.isEmpty()) {
                builder.up().withDetail("brokers", nodes.size());
            } else {
                builder.down().withDetail("error", "No brokers available");
            }
        }
    }
}