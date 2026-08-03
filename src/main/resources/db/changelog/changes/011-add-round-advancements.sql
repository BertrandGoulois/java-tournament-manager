-- changeset bertrand:11
-- Sert de verrou distribué fiable pour AdvanceBracketService : avant de créer les matchs
-- d'un round, on tente d'insérer un marqueur (tournament_id, round). La contrainte
-- d'unicité fait échouer toute tentative concurrente ou redondante (redelivery Kafka
-- at-least-once) de créer le même round deux fois — contrairement à un simple check
-- applicatif ("le round existe-t-il déjà ?"), qui reste une condition de course
-- (check-then-act) entre deux transactions concurrentes.
CREATE TABLE round_advancements (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id),
    round INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tournament_id, round)
);
