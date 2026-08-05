-- changeset bertrand:16
-- Toutes les dates de l'application étaient des LocalDateTime mappées sur TIMESTAMP (sans
-- fuseau) : une source de bugs assurée pour une application avec un job planifié en cron,
-- une politique de rétention à 30 jours, et un déploiement potentiellement dans une autre
-- zone horaire (l'heure "murale" stockée perd tout ancrage absolu, deux instances dans des
-- fuseaux différents ne s'accordent plus sur "il y a 30 jours").
--
-- Bascule vers TIMESTAMPTZ (timestamp ancré dans le temps, indépendant du fuseau du serveur
-- qui le lit ou l'écrit) côté base, et Instant côté Java (voir les entités correspondantes).
-- Les valeurs déjà en base sont réinterprétées comme UTC (hypothèse raisonnable : le serveur
-- applicatif tourne en UTC — image Docker openjdk standard sans TZ personnalisée).
ALTER TABLE players ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE players ALTER COLUMN deleted_at TYPE TIMESTAMPTZ USING deleted_at AT TIME ZONE 'UTC';
ALTER TABLE players ALTER COLUMN anonymized_at TYPE TIMESTAMPTZ USING anonymized_at AT TIME ZONE 'UTC';

ALTER TABLE tournaments ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE tournaments ALTER COLUMN deleted_at TYPE TIMESTAMPTZ USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE registrations ALTER COLUMN registered_at TYPE TIMESTAMPTZ USING registered_at AT TIME ZONE 'UTC';

ALTER TABLE matches ALTER COLUMN played_at TYPE TIMESTAMPTZ USING played_at AT TIME ZONE 'UTC';

ALTER TABLE elo_history ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE refresh_tokens ALTER COLUMN expiry_date TYPE TIMESTAMPTZ USING expiry_date AT TIME ZONE 'UTC';
ALTER TABLE refresh_tokens ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE round_advancements ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE outbox_events ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE outbox_events ALTER COLUMN published_at TYPE TIMESTAMPTZ USING published_at AT TIME ZONE 'UTC';
