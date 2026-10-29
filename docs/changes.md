# Historial de cambios técnicos

Más reciente primero. El detalle completo está en `git log`.

## Firmware DHT11 con Wi-Fi y MQTT (sin commit en este repositorio)

- Actualizado el estado del firmware en `mqtt-contract.md`, `CLAUDE.md` y `README.md`: compila, no probado, QoS 0.

## Documentación (`469b390`)

- Se agregan README y documentos en `docs/`, el agente `technical-writer`, la skill `update-documentation` y la regla de documentación obligatoria en `CLAUDE.md`.

## Nuevo contrato MQTT (`9d266d8`)

- Payload con `deviceId`, `messageId`, `timestamp` ISO-8601 UTC y `measurements`. `deviceId` debe coincidir con el tópico. Se elimina el campo `v`.
- Migración `V2`: columna `message_id`; la clave única pasa a (`message_id`, `ts`). Probada sobre datos existentes con el Compose local.
- Pruebas: 37 pasan (34 unitarias, 3 de integración).

## Puerto de TimescaleDB configurable (`6d6145c`)

- `POSTGRES_PORT` en `.env`, usado por `docker-compose.yml` y `application.yaml`, para evitar conflictos con otro PostgreSQL local.
- Probado a mano con el Compose real: autenticación MQTT, persistencia en volumen y reconexión.

## Recepción MQTT y persistencia (`57b1899`)

- Suscripción a `fvh/+/lecturas` (QoS 1, sesión persistente, reconexión automática), validación y persistencia con `JdbcTemplate`.
- Migración `V1`: tabla `lecturas` como hypertable.
- Pruebas unitarias y de integración con Testcontainers.

## Configuración inicial (`a947839`)

- Agentes `backend-architect`, `spring-boot-developer`, `mqtt-reviewer` y skills `mqtt-message-contract`, `database-migration`, `backend-testing`.
- `docker-compose.yml` con TimescaleDB y Mosquitto (con autenticación). Java 21.
