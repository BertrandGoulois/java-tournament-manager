package com.tournament.tournament_manager.config.kafka;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka de l'application.
 *
 * <p>Déclare un seul topic : {@code match-finished}, consommé par trois
 * consumer groups indépendants ({@code elo-group}, {@code bracket-group},
 * {@code websocket-group}), ce qui garantit que chaque listener reçoit
 * tous les messages indépendamment des autres.
 *
 * <p>Sérialisation : clé en {@code String}, valeur en JSON via Jackson.
 * Le type de désérialisation par défaut est {@code MatchFinishedEvent} —
 * tous les packages sont marqués comme trusted ({@code TRUSTED_PACKAGES = "*"}).
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    /**
     * Nom du topic Kafka publié par {@code MatchService}
     * et consommé par les trois listeners.
     */
    public static final String MATCH_FINISHED_TOPIC = "match-finished";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Crée et configure le {@code KafkaAdmin} avec auto-création des topics activée.
     * Les topics déclarés comme beans {@code NewTopic} sont créés automatiquement
     * au démarrage si absents.
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        KafkaAdmin admin = new KafkaAdmin(config);
        admin.setAutoCreate(true);
        return admin;
    }

    /**
     * Déclare le topic {@code match-finished} avec 1 partition et 1 réplica.
     * Convient pour un environnement de développement — à augmenter en production.
     */
    @Bean
    public NewTopic matchFinishedTopic() {
        return TopicBuilder.name(MATCH_FINISHED_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Configure le producteur Kafka.
     * Clé : {@code StringSerializer}, valeur : {@code JacksonJsonSerializer}.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * Fournit le {@code KafkaTemplate} utilisé par {@code MatchService}
     * pour publier les événements.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Configure le consommateur Kafka.
     * Clé : {@code StringDeserializer}, valeur : {@code JacksonJsonDeserializer}
     * avec {@code MatchFinishedEvent} comme type cible par défaut.
     *
     * <p>Le {@code GROUP_ID_CONFIG} ici ({@code "elo-group"}) sert de valeur par défaut
     * — chaque listener surcharge son propre {@code groupId} via {@code @KafkaListener}.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "elo-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, "com.tournament.tournament_manager.domain.event.MatchFinishedEvent");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Fournit la factory de containers utilisée par les annotations {@code @KafkaListener}.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}