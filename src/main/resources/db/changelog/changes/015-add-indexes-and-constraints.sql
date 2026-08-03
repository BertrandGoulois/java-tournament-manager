-- changeset bertrand:15
-- PostgreSQL n'indexe pas automatiquement les colonnes de clé étrangère. Sans ces index,
-- toute requête filtrant sur ces colonnes fait un scan complet de la table concernée.
-- findByTournamentIdAndRound (appelé à chaque fin de match, dans AdvanceBracketService)
-- est le cas le plus chaud : sans idx_matches_tournament_round, c'est un seq scan sur
-- matches à chaque avancement de bracket.
CREATE INDEX idx_matches_tournament ON matches(tournament_id);
CREATE INDEX idx_matches_tournament_round ON matches(tournament_id, round);
CREATE INDEX idx_registrations_tournament ON registrations(tournament_id);
CREATE INDEX idx_elo_history_player ON elo_history(player_id);
CREATE INDEX idx_matches_winner ON matches(winner_id);

-- player1_id et player2_id : requêtées à chaque calcul de stats joueur
-- (countFinishedRealMatchesByPlayer, countRealWinsByPlayer — voir MatchRepository).
CREATE INDEX idx_matches_player1 ON matches(player1_id);
CREATE INDEX idx_matches_player2 ON matches(player2_id);

-- Sans cette contrainte, la garde d'idempotence existsByMatchId() d'EloListener reste une
-- condition de course (check-then-act) entre le moment où elle est évaluée et le moment où
-- les deux lignes d'historique sont réellement insérées : deux exécutions concurrentes du
-- même événement (redelivery Kafka sur des threads différents) pourraient toutes les deux
-- passer le check, puis toutes les deux insérer, doublant la variation ELO appliquée.
-- Avec la contrainte, la seconde insertion échoue proprement plutôt que de dupliquer
-- silencieusement l'historique (voir EloService, qui rattrape ce cas).
ALTER TABLE elo_history ADD CONSTRAINT uq_elo_history_match_player UNIQUE (match_id, player_id);
