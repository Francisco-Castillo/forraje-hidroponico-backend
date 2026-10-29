CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE lecturas (
    ts            timestamptz      NOT NULL,
    nodo          text             NOT NULL,
    temperatura_c double precision NOT NULL,
    humedad_pct   double precision NOT NULL,
    PRIMARY KEY (nodo, ts)
);

SELECT create_hypertable('lecturas', 'ts');
