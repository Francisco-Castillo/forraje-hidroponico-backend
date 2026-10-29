ALTER TABLE lecturas ADD COLUMN message_id text;

UPDATE lecturas
SET message_id = 'migrado-' || nodo || '-' || extract(epoch FROM ts)::bigint;

ALTER TABLE lecturas ALTER COLUMN message_id SET NOT NULL;

-- Una hypertable exige que la clave única incluya la columna de tiempo.
ALTER TABLE lecturas DROP CONSTRAINT lecturas_pkey;
ALTER TABLE lecturas ADD PRIMARY KEY (message_id, ts);
