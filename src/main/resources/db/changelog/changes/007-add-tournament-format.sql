--liquibase formatted sql

--changeset bertrand:007-add-tournament-format
ALTER TABLE tournaments ADD COLUMN format VARCHAR(30) NOT NULL DEFAULT 'SINGLE_ELIMINATION';