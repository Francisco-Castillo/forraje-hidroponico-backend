-- Valor crudo del ADC (0-4095) del sensor capacitivo de suelo. Opcional: los mensajes sin el campo dejan NULL.
-- No es un porcentaje: aun no hay calibracion.
ALTER TABLE lecturas ADD COLUMN IF NOT EXISTS humedad_suelo_raw integer;
