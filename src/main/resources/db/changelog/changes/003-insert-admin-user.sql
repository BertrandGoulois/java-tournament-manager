-- changeset bertrand:003

INSERT INTO users (id, username, password, role, created_at)
VALUES (1, 'admin', '$2a$12$tKWA7GKVtM6ti5vQIwnm9.k5.h/XsHmmMu30cierOor2I16QDLXLi', 'ADMIN', NOW());