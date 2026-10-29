-- Consultas de la API por dispositivo y tiempo (la clave primaria es (message_id, ts)).
CREATE INDEX IF NOT EXISTS lecturas_nodo_ts_idx ON lecturas (nodo, ts DESC);
