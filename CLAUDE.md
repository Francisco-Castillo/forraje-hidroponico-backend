# Backend sensores (forraje verde hidropónico)

## Objetivo

Backend Spring Boot que recibe por MQTT las mediciones de un ESP32, las valida y las guarda en TimescaleDB. Más adelante: más sensores, actuadores, reglas de automatización y consultas históricas.

## Stack

- Java 21, Spring Boot 4.1.2, Maven (`./mvnw`).
- MQTT: `spring-integration-mqtt` (Paho) contra Eclipse Mosquitto.
- Persistencia: escritura (ingesta MQTT) con `JdbcTemplate`; lectura (API REST) con JPA, entidad inmutable y `Specification`; sobre TimescaleDB (PostgreSQL). Migraciones con Flyway.
- API REST: Spring MVC, DTOs + MapStruct, OpenAPI con springdoc (Swagger UI en `/swagger-ui.html`). Controladores y servicios con interfaz e implementación.
- Infraestructura local: `docker-compose.yml` (`timescaledb`, `mosquitto`), independiente del proceso Spring Boot. Datos en volúmenes con nombre. Variables en `.env` (modelo: `.env.example`).
- Firmware publicador: proyecto PlatformIO `DHT11` (ESP32).

## Decisiones aprobadas

- Mensajes JSON con una lectura completa por mensaje, timestamp ISO-8601 UTC generado por el ESP32, `messageId` único para deduplicar, QoS 1.
- Broker con usuario y contraseña (`allow_anonymous false`). El archivo `docker/mosquitto/passwd` no se versiona.
- Contrato de tópicos y campos: `docs/mqtt-contract.md`.
- La API de consulta es de solo lectura y sin autenticación por ahora (decisión aprobada); contrato en `docs/api.md`.
- JPA solo para lectura (aprobado 2026-10-29); la ingesta sigue con `JdbcTemplate`.

## Estado

- Implementado: migraciones `V1` (hypertable `lecturas`) y `V2` (`message_id`), suscripción MQTT a `fvh/+/lecturas`, validación según el contrato y persistencia con `JdbcTemplate` (duplicados ignorados por `message_id`). Código en `com.fvh.sensores.lectura` y `com.fvh.sensores.mqtt`.
- API de consulta implementada (`com.fvh.sensores.lectura.api` y `.consulta`), migración `V3`, probada contra la base real.
- Campo opcional `soilMoistureRaw` (sensor de suelo, crudo): migración `V4`, parser, persistencia y API; 78 pruebas pasan. `V4` aplicada a la base del Compose y verificada con el ESP32 real (2026-10-29).
- Pruebas (`./mvnw test`): las de integración usan Testcontainers y requieren Docker en marcha.
- Probado a mano con el Compose local y credenciales: mensajes por `mosquitto_pub` llegan a la base; el broker rechaza conexiones sin credenciales; los datos sobreviven a `docker compose stop/up` y el backend se reconecta.
- Parcialmente probado: el firmware ESP32 (publica con QoS 0). Camino feliz verificado en hardware el 2026-10-29 contra el broker y la base; casos de fallo sin probar.
- Limitación conocida: si falla la escritura en la base, el mensaje se pierde (se confirma al broker igualmente; sin reintento).

## Reglas

1. Inspecciona el proyecto antes de modificarlo.
2. Presenta un plan breve y espera aprobación antes de modificar archivos.
3. Reutiliza las dependencias existentes y evita abstracciones innecesarias.
4. No inventes versiones, configuraciones ni requisitos: pregunta o verifica.
5. No ejecutes comandos destructivos (`docker compose down -v`, borrar volúmenes o datos) sin autorización.
6. Distingue entre código escrito, compilación correcta y pruebas realmente ejecutadas, e informa cada estado por separado.
7. **Documentación obligatoria.** Toda tarea que modifique código, configuración, esquema de datos, mensajes MQTT, pruebas o comportamiento del sistema incluye revisar la documentación afectada antes de darla por terminada, aplicando la skill `update-documentation` (la documentación no se deja para otra sesión). Al cerrar, informa qué documentos actualizaste o por qué no hacía falta.

## Agentes y skills

- Agentes (`.claude/agents/`): `backend-architect` (diseña), `spring-boot-developer` (implementa), `mqtt-reviewer` (revisa contratos MQTT), `technical-writer` (documenta).
- Skills (`.claude/skills/`): `mqtt-message-contract`, `database-migration`, `backend-testing`, `update-documentation`.

## Puesta en marcha local

Ver [docs/development-setup.md](docs/development-setup.md). Índice de la documentación en [README.md](README.md).
