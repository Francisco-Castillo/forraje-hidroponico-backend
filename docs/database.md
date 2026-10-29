# Base de datos

TimescaleDB 2.31.0 sobre PostgreSQL 17 (imagen `timescale/timescaledb:2.31.0-pg17`). Acceso con JDBC (`JdbcTemplate`). El esquema lo gestiona Flyway al arrancar el backend.

## Esquema

Tabla `lecturas`, hypertable particionada por `ts`:

| Columna | Tipo | Notas |
|---|---|---|
| `ts` | `timestamptz` | `timestamp` del mensaje, UTC. Clave de partición. |
| `nodo` | `text` | `deviceId`, igual al `<nodo>` del tópico. |
| `temperatura_c` | `double precision` | °C |
| `humedad_pct` | `double precision` | % de humedad relativa |
| `message_id` | `text` | Identificador de la lectura. |

Clave primaria: (`message_id`, `ts`). TimescaleDB exige que las claves únicas de una hypertable incluyan la columna de tiempo. Las inserciones usan `ON CONFLICT (message_id, ts) DO NOTHING`.

## Migraciones

Ruta: `src/main/resources/db/migration/`.

| Versión | Archivo | Cambio |
|---|---|---|
| 1 | `V1__crear_lecturas.sql` | Extensión `timescaledb`, tabla `lecturas` e hypertable. |
| 2 | `V2__agregar_message_id.sql` | Agrega `message_id` (las filas previas reciben `migrado-<nodo>-<epoch>`) y cambia la clave primaria. |

Reglas (detalle en la skill `database-migration`): nunca editar una migración ya aplicada; crear un archivo nuevo `V<n>__descripcion.sql`; preferir cambios aditivos; no borrar datos sin autorización.

## Consultas útiles (Verificado)

Se ejecutan con `docker compose exec -T timescaledb sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "<consulta>"'`.

```sql
-- últimas lecturas
SELECT message_id, nodo, ts, temperatura_c, humedad_pct FROM lecturas ORDER BY ts DESC LIMIT 10;

-- promedio por hora y nodo
SELECT time_bucket(INTERVAL '1 hour', ts) AS hora, nodo, round(avg(temperatura_c)::numeric, 2) AS temp_media, count(*)
FROM lecturas GROUP BY hora, nodo ORDER BY hora DESC LIMIT 5;

-- confirmar la hypertable
SELECT hypertable_name FROM timescaledb_information.hypertables;

-- migraciones aplicadas
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

Para entrar de forma interactiva: `docker compose exec timescaledb sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'`.

## Retención y compresión

No definidas: nadie las ha pedido todavía.
