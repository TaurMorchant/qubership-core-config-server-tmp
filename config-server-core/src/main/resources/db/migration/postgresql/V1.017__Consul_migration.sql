CREATE TABLE if not exists consul_migrated
(
    m bool
);

INSERT INTO consul_migrated
SELECT false WHERE NOT EXISTS (SELECT 1 FROM consul_migrated);