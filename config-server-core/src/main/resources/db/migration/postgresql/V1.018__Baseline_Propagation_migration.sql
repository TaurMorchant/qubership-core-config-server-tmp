CREATE TABLE if not exists baseline_migrated
(
    m bool
);

INSERT INTO baseline_migrated
SELECT false WHERE NOT EXISTS (SELECT 1 FROM baseline_migrated);