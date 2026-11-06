ALTER TABLE corsi ADD name_normalized VARCHAR(100);

UPDATE corsi SET name_normalized = LOWER(TRIM(name));

ALTER TABLE corsi ALTER COLUMN name_normalized SET NOT NULL;

ALTER TABLE corsi ADD CONSTRAINT uk_corsi_name_normalized UNIQUE (name_normalized);
