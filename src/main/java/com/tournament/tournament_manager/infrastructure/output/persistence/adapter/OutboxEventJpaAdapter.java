package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.port.out.maintenance.PurgeOutboxEventsPort;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Adapter JPA implémentant la purge des événements outbox déjà publiés.
 *
 * <p>Séparé des autres adaptateurs de l'outbox ({@code MatchKafkaAdapter} pour l'écriture,
 * {@code OutboxPublisherService} pour la publication elle-même) : cette classe n'a qu'une
 * seule responsabilité, purement liée à la purge.
 */
@Component
public class OutboxEventJpaAdapter implements PurgeOutboxEventsPort {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventJpaAdapter(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public int deletePublishedBefore(Instant before) {
        return outboxEventRepository.deletePublishedBefore(before);
    }
}
