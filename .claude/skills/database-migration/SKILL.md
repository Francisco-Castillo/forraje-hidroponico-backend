---
name: database-migration
description: Procedimiento para gestionar cambios de esquema mediante migraciones Flyway versionadas, preservando los datos existentes. Usar al crear o modificar tablas, índices o hypertables.
---

# Migraciones de base de datos

Las migraciones están en `src/main/resources/db/migration` (Flyway, PostgreSQL con TimescaleDB).

1. **Alcance.** Indica qué cambia en el esquema y qué datos existentes se ven afectados.
2. **Estado actual.** Lee las migraciones existentes y confirma el siguiente número de versión (`V<n>__descripcion.sql`).
3. **Nueva migración.** Crea un archivo nuevo. Nunca edites ni renombres una migración ya aplicada.
4. **Datos.** Prefiere cambios aditivos (columnas nuevas, nulas o con valor por defecto). Para renombrar o eliminar, usa pasos separados que conserven los datos y pide autorización antes de borrar columnas, tablas o datos.
5. **TimescaleDB.** Las hypertables (`create_hypertable`) se crean en la migración, con la columna de tiempo como clave. No supongas políticas de retención o compresión sin que el usuario las pida.
6. **Verificación.** Aplica la migración sobre una base vacía y sobre una con datos, con la skill `backend-testing`, e informa qué se ejecutó realmente.

## Reglas

- No ejecutes `flyway clean`, `docker compose down -v` ni borres volúmenes sin autorización.
- Los cambios de esquema que afecten a un contrato de mensajes se coordinan con `mqtt-message-contract`.
