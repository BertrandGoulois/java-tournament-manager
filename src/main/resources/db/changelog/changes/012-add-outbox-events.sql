-- changeset bertrand:12
-- Pattern Transactional Outbox : au lieu d'envoyer directement à Kafka pendant la
-- transaction métier (RecordMatchResultService), on écrit une ligne ici, dans la MÊME
-- transaction que la mise à jour du match. Écriture DB + écriture outbox commitent ou
-- rollback ensemble — atomique par construction, contrairement à un envoi Kafka direct
-- qui peut réussir alors que la transaction rollback ensuite (ou l'inverse).
--
-- Un poller séparé (OutboxPublisherService) publie ensuite réellement vers Kafka, en
-- dehors de toute transaction métier : une panne Kafka retarde la publication sans jamais
-- perdre l'événement, puisqu'il reste en base jusqu'à confirmation d'envoi.
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    partition_key VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP
);

-- Index partiel : ne couvre que les lignes non publiées, celles que le poller interroge
-- à chaque cycle. Reste petit et efficace même si la table grossit (les lignes publiées
-- s'accumulent jusqu'à leur purge périodique, voir PurgeService).
CREATE INDEX idx_outbox_events_unpublished ON outbox_events(id) WHERE published_at IS NULL;
