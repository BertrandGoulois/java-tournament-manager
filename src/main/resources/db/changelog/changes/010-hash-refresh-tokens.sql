-- changeset bertrand:10
-- À partir de cette version, la colonne "token" stocke un hash SHA-256 (hex, 64
-- caractères) du refresh token, plus la valeur brute en clair (voir RefreshTokenService).
-- Les tokens déjà en base ont été persistés en clair : impossible de les hasher a
-- posteriori côté serveur puisqu'on ne connaît plus leur valeur brute pour comparaison
-- future. On les invalide donc tous — chaque utilisateur devra simplement se reconnecter
-- une fois ; c'est le comportement attendu après un durcissement de ce type.
DELETE FROM refresh_tokens;
