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
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

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
 * Le type de désérialisation est fixé à {@code MatchFinishedEvent} — seul le
 * package {@code com.tournament.tournament_manager.domain.event} est marqué comme
 * trusted, et l'en-tête {@code __TypeId__} (potentiellement forgeable par tout
 * producteur autorisé à publier sur le topic) est ignoré au profit de ce type fixe.
 *
 * <p>En cas d'échec répété d'un listener (3 tentatives espacées de 1 seconde),
 * le message est redirigé vers le topic {@code match-finished.DLT}
 * (Dead Letter Topic) pour inspection et rejeu manuel.
 *
 * <p><b>Source unique de vérité (point 2.4 de la revue).</b> Cette classe déclare
 * explicitement {@code ProducerFactory} et {@code ConsumerFactory}, ce qui fait reculer
 * l'auto-configuration Spring Boot : toutes les propriétés {@code spring.kafka.producer.*}
 * et {@code spring.kafka.consumer.*} étaient donc <b>sans aucun effet</b>. Elles étaient
 * pourtant présentes dans {@code application.properties}, dupliquant ce qui est codé ici et
 * donnant l'illusion qu'on pouvait régler Kafka sans toucher au Java.
 *
 * <p>Ce n'était pas qu'une redondance esthétique : {@code auto-offset-reset=earliest} était
 * déclaré dans les propriétés et n'était repris nulle part ici, si bien que les consommateurs
 * tournaient en réalité sur le défaut Kafka ({@code latest}). Un nouveau groupe de
 * consommateurs ignorait silencieusement tout l'historique du topic — l'inverse exact de ce
 * que la configuration affichait. Le réglage est désormais appliqué pour de bon, ci-dessous.
 *
 * <p>Seul {@code spring.kafka.bootstrap-servers} reste dans les propriétés : il varie par
 * environnement (localhost en dev, {@code kafka:29092} en docker) et est lu ici via
 * {@code @Value}. Tout le reste se règle dans cette classe.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    public static final String MATCH_FINISHED_TOPIC = "match-finished";
    public static final String MATCH_FINISHED_DLT = "match-finished.DLT";
    public static final String ELO_GROUP = "elo-group";
    public static final String BRACKET_GROUP = "bracket-group";
    public static final String WEBSOCKET_GROUP = "websocket-group";
    public static final String DLT_GROUP = "dlt-group";
    public static final String COMMENTARY_GROUP = "commentary-group";

    /** Seul package dont la desérialisation est autorisée — voir TRUSTED_PACKAGES. */
    private static final String EVENT_PACKAGE = "com.tournament.tournament_manager.domain.event";

    /**
     * Nombre de tentatives avant redirection vers la DLQ.
     */
    private static final long MAX_ATTEMPTS = 3;

    /**
     * Délai en millisecondes entre chaque tentative.
     */
    private static final long BACK_OFF_INTERVAL = 1000L;

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
     * Déclare le topic Dead Letter {@code match-finished.DLT}.
     * Reçoit les messages qui ont échoué après {@code MAX_ATTEMPTS} tentatives.
     */
    @Bean
    public NewTopic matchFinishedDltTopic() {
        return TopicBuilder.name(MATCH_FINISHED_TOPIC + ".DLT")
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
     * Fournit le {@code KafkaTemplate} utilisé par {@code RecordMatchResultService}
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
     * <p>Le {@code GROUP_ID_CONFIG} ici ({@link #ELO_GROUP}) sert de valeur par défaut
     * — chaque listener surcharge son propre {@code groupId} via {@code @KafkaListener}.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, ELO_GROUP);
        // Point 2.4 : ce reglage etait declare dans application.properties, ou il n'avait
        // aucun effet (l'auto-configuration recule devant ce bean). Les consommateurs
        // tournaient donc sur "latest" et un nouveau groupe sautait tout l'historique du
        // topic. Applique ici, il fait enfin ce qu'il annonce.
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, EVENT_PACKAGE);
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, EVENT_PACKAGE + ".MatchFinishedEvent");
        // Le topic ne véhicule qu'un seul type d'événement : on ignore l'en-tête __TypeId__
        // (que tout producteur capable de publier sur le topic pourrait sinon forger) et on
        // force systématiquement la désérialisation vers VALUE_DEFAULT_TYPE.
        config.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Configure le gestionnaire d'erreurs avec redirection vers la DLQ.
     *
     * <p>{@code FixedBackOff} : {@code MAX_ATTEMPTS} tentatives espacées de
     * {@code BACK_OFF_INTERVAL} ms. Après épuisement des tentatives,
     * {@code DeadLetterPublishingRecoverer} redirige le message vers
     * {@code match-finished.DLT}.
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        return new DefaultErrorHandler(recoverer, new FixedBackOff(BACK_OFF_INTERVAL, MAX_ATTEMPTS));
    }

    /**
     * Fournit la factory de containers utilisée par les annotations {@code @KafkaListener}.
     * Intègre le gestionnaire d'erreurs avec DLQ.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}