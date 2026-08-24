-- changeset bertrand:17
-- Table requise par ShedLock (schéma standard imposé par la librairie, ne pas modifier les
-- noms/types de colonnes). Sans verrou distribué, un déploiement multi-instances exécuterait
-- le job de purge N fois en parallèle à 2h du matin — inoffensif au sens strict (les
-- DELETE/UPDATE sont idempotents), mais source de travail redondant et de logs confus.
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
