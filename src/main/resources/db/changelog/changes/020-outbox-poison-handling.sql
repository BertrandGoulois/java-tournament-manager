-- changeset bertrand:20

-- Point 2.3 de la revue, second volet. Un evenement dont le payload est illisible etait
-- retente indefiniment, toutes les 500 ms, avec un log.error a chaque passage : il inondait
-- les logs sans jamais progresser, et occupait une place dans chaque lot au detriment des
-- evenements sains qui, eux, auraient pu partir.
--
-- Trois colonnes rendent l'abandon representable :
--   attempts   : nombre de tentatives echouees, pour l'observabilite
--   failed_at  : horodatage de l'abandon definitif (NULL = evenement encore actif)
--   last_error : cause de l'abandon, pour ne pas avoir a fouiller les logs
--
-- ATTENTION - l'abandon n'est PAS declenche par un compteur. Le poller tourne toutes les
-- 500 ms : une panne Kafka de trois secondes suffirait a epuiser n'importe quel seuil de
-- tentatives raisonnable et a jeter TOUS les evenements en attente, transformant un incident
-- passager en perte de donnees definitive. Seules les erreurs non rejouables (payload
-- illisible, message trop gros, topic invalide) mettent failed_at ; une panne
-- d'infrastructure laisse l'evenement en attente indefiniment, ce qui est le comportement
-- correct pour un outbox. Voir OutboxPublisherService.isPermanentFailure.
ALTER TABLE outbox_events
    ADD COLUMN attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN failed_at TIMESTAMPTZ,
    ADD COLUMN last_error TEXT;

-- L'index partiel du poller doit maintenant exclure les evenements abandonnes, sans quoi
-- ils continueraient d'etre verrouilles et relus a chaque cycle - soit exactement le
-- gaspillage qu'on supprime.
DROP INDEX IF EXISTS idx_outbox_events_unpublished;
CREATE INDEX idx_outbox_events_unpublished
    ON outbox_events (id)
    WHERE published_at IS NULL AND failed_at IS NULL;

-- Index dedie aux evenements abandonnes : ils sont rares, mais on veut pouvoir les lister
-- instantanement pour diagnostic et pour alimenter la metrique de supervision.
CREATE INDEX idx_outbox_events_failed
    ON outbox_events (failed_at)
    WHERE failed_at IS NOT NULL;
