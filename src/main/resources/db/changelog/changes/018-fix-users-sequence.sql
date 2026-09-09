-- changeset bertrand:18

-- Point 1.5 de la revue. La migration 003 insere l'admin avec un id explicite (id = 1),
-- ce qui ne fait PAS avancer la sequence BIGSERIAL adossee a users.id : celle-ci vaut
-- toujours 1. Latent tant qu'aucun code ne cree d'utilisateur, mais la premiere insertion
-- sans id explicite echouera sur
--     duplicate key value violates unique constraint "users_pkey"
-- On resynchronise la sequence sur le maximum reellement present.
--
-- La migration 003 elle-meme n'est pas modifiee : elle a deja tourne sur les bases
-- existantes et Liquibase refuserait le changement de checksum.
SELECT setval(
    pg_get_serial_sequence('users', 'id'),
    COALESCE((SELECT MAX(id) FROM users), 1)
);
