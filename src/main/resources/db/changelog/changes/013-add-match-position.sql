-- changeset bertrand:13
-- Sans position, un match n'a pas de place déterminée dans le tableau : le tirage était
-- refait aléatoirement à chaque tour (Collections.shuffle), donc aucune structure d'arbre
-- réelle, aucun seeding. La position (0-indexée au sein d'un round) rend le bracket
-- déterministe : le vainqueur des matchs aux positions 2k et 2k+1 d'un round avance vers
-- la position k du round suivant — plus besoin de retirage.
ALTER TABLE matches ADD COLUMN position INT NOT NULL DEFAULT 0;
