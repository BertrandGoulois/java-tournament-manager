-- changeset bertrand:19

-- Point 2.2 de la revue : le claim de round est pris AVANT la creation des matchs, dans une
-- transaction independante (REQUIRES_NEW, necessaire pour que la contrainte d'unicite soit
-- visible des concurrents avant le commit metier). Si la transaction metier echoue ensuite,
-- elle est annulee mais le claim, lui, est deja commite : le round est reserve pour un
-- travail qui n'a jamais eu lieu. Plus personne ne recreera jamais ce round, et une
-- redelivery Kafka tombera sur "round deja reclame" puis sortira en SUCCES. Le tournoi est
-- bloque definitivement, sans la moindre erreur visible.
--
-- La colonne 'status' rend cet etat intermediaire representable :
--   PENDING = claim pris, creation des matchs pas encore confirmee
--   DONE    = matchs crees et transaction metier commitee
--
-- Un claim qui reste PENDING est, par construction, la trace d'un echec.
--
-- DEFAULT 'DONE' pour les lignes existantes : elles correspondent a des rounds effectivement
-- crees (le schema precedent ne permettait pas d'en douter). Les marquer PENDING ferait
-- croire au job de reconciliation qu'il doit toutes les liberer, ce qui autoriserait la
-- recreation de rounds deja joues.
ALTER TABLE round_advancements
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DONE';

-- Le DEFAULT n'avait de sens que pour la migration des lignes existantes. On le retire pour
-- que toute nouvelle insertion soit obligee de declarer son statut : un INSERT qui oublierait
-- le statut retomberait silencieusement sur 'DONE' et reintroduirait exactement le bug.
ALTER TABLE round_advancements
    ALTER COLUMN status DROP DEFAULT;

-- Index partiel : le job de reconciliation ne cherche QUE des PENDING, qui sont l'exception
-- (une poignee de lignes au plus, contre potentiellement des milliers de DONE).
CREATE INDEX idx_round_advancements_pending
    ON round_advancements (created_at)
    WHERE status = 'PENDING';
