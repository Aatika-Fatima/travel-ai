CREATE TABLE airports (
    id BIGSERIAL PRIMARY KEY,
    duffel_id VARCHAR(64) NOT NULL,
    iata_code VARCHAR(3) NOT NULL,
    icao_code VARCHAR(4),
    name VARCHAR(255) NOT NULL,
    city_name VARCHAR(255),
    iata_city_code VARCHAR(3),
    iata_country_code VARCHAR(2),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    time_zone VARCHAR(64),
    last_synced_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX ux_airports_duffel_id ON airports (duffel_id);
CREATE UNIQUE INDEX ux_airports_iata_code ON airports (iata_code);

CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX ix_airports_name_trgm ON airports USING gin (name gin_trgm_ops);
CREATE INDEX ix_airports_city_name_trgm ON airports USING gin (city_name gin_trgm_ops);
