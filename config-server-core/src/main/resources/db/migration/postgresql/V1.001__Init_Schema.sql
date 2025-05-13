CREATE TABLE migrated (
    m bool
);

INSERT INTO migrated  (m) VALUES (false);

CREATE TABLE config_profile
(
    id          uuid NOT NULL,
    application text,
    profile     text,
    version     int,
    CONSTRAINT config_profile_pk PRIMARY KEY (id)
);

CREATE TABLE config_property
(
    id        uuid NOT NULL,
    "key"       text,
    "value"     text,
    encrypted bool,
    CONSTRAINT config_property_pk PRIMARY KEY (id)
);

CREATE TABLE config_profile_config_property
(
    config_profile_id uuid NOT NULL,
    properties_id     uuid NOT NULL,
    FOREIGN KEY (config_profile_id) REFERENCES config_profile (id),
    FOREIGN KEY (properties_id) REFERENCES config_property (id)
);
