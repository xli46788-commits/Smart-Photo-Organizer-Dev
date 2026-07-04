USE imprint;

ALTER TABLE user_category ADD COLUMN keywords VARCHAR(500) NULL AFTER name;
