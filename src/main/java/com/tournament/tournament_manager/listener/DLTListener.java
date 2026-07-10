package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consomme les événements en échec depuis le topic {@code match-finished.DLT}.
 *
 * <p>Chaque message qui arrive ici a échoué après {@code MAX_ATTEMPTS} tentatives
 * dans les listeners principaux. Le message est loggé pour permettre
 * l'investigation et le rejeu manuel via Kafka UI.
 */
@Slf4j
@Component
public class DLTListener {

    /**
     * Reçoit les messages en échec et les logue pour investigation.
     *
     * @param event l'événement contenant l'identifiant du match en échec
     */
    @KafkaListener(topics = KafkaConfig.MATCH_FINISHED_DLT, groupId = KafkaConfig.DLT_GROUP)
    public void onDeadLetter(MatchFinishedEvent event) {
        log.error("Message en échec dans la DLT - matchId={} : ce match n'a pas été traité correctement. " +
                "Vérifier les logs et rejouer via Kafka UI si nécessaire.", event.matchId());
    }
}