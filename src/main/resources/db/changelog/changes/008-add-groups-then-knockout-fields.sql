-- changeset bertrand:8
ALTER TABLE matches ADD COLUMN group_number INTEGER;
ALTER TABLE tournaments ADD COLUMN number_of_groups INTEGER;
ALTER TABLE tournaments ADD COLUMN qualifiers_per_group INTEGER;