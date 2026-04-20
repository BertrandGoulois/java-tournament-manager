-- changeset bertrand:001

CREATE TABLE players (
                         id BIGSERIAL PRIMARY KEY,
                         username VARCHAR(255) NOT NULL UNIQUE,
                         email VARCHAR(255) NOT NULL UNIQUE,
                         elo_rating INT NOT NULL DEFAULT 1000,
                         created_at TIMESTAMP NOT NULL
);

CREATE TABLE tournaments (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(255) NOT NULL UNIQUE,
                             status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
                             max_players INT NOT NULL,
                             created_at TIMESTAMP NOT NULL
);

CREATE TABLE registrations (
                               id BIGSERIAL PRIMARY KEY,
                               registered_at TIMESTAMP NOT NULL,
                               player_id BIGINT NOT NULL REFERENCES players(id),
                               tournament_id BIGINT NOT NULL REFERENCES tournaments(id),
                               UNIQUE(player_id, tournament_id)
);

CREATE TABLE matches (
                         id BIGSERIAL PRIMARY KEY,
                         round INT NOT NULL,
                         status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                         played_at TIMESTAMP,
                         tournament_id BIGINT NOT NULL REFERENCES tournaments(id),
                         player1_id BIGINT NOT NULL REFERENCES players(id),
                         player2_id BIGINT REFERENCES players(id),
                         winner_id BIGINT REFERENCES players(id)
);

CREATE TABLE elo_history (
                             id BIGSERIAL PRIMARY KEY,
                             elo_change INT NOT NULL,
                             elo_after INT NOT NULL,
                             created_at TIMESTAMP NOT NULL,
                             player_id BIGINT NOT NULL REFERENCES players(id),
                             match_id BIGINT NOT NULL REFERENCES matches(id)
);