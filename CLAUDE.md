# Backend sensores (forraje verde hidropónico)

## Objetivo

Backend Spring Boot que recibe por MQTT las mediciones de un ESP32, las valida y las guarda en TimescaleDB. Más adelante: más sensores, actuadores, reglas de automatización y consultas históricas.

## Stack

- Java 21, Spring Boot 4.1.2, Maven (`./mvnw`).
- MQTT: `spring-integration-mqtt` (Paho) contra Eclipse Mosquitto.
- Persistencia: JDBC con `JdbcTemplate` sobre TimescaleDB (PostgreSQL). Migraciones con Flyway.
- Infraestructura local: `docker-compose.yml` (`timescaledb`, `mosquitto`), independiente del proceso Spring Boot. Datos en volúmenes con nombre. Variables en `.env` (modelo: `.env.example`).
- Firmware publicador: proyecto PlatformIO `DHT11` (ESP32).

## Decisiones aprobadas

- Mensajes JSON con una lectura completa por mensaje, timestamp generado por el ESP32, QoS 1.
- Broker con usuario y contraseña (`allow_anonymous false`). El archivo `docker/mosquitto/passwd` no se versiona.
- Contrato de tópicos y campos: `docs/mqtt-contract.md`.

## Estado

- Implementado: migración `V1` (hypertable `lecturas`), suscripción MQTT a `fvh/+/lecturas`, validación según el contrato y persistencia con `JdbcTemplate` (duplicados ignorados por `nodo, ts`). Código en `com.fvh.sensores.lectura` y `com.fvh.sensores.mqtt`.
- Pruebas (`./mvnw test`): las de integración usan Testcontainers y requieren Docker en marcha.
- Probado a mano con el Compose local y credenciales: mensajes por `mosquitto_pub` llegan a la base; el broker rechaza conexiones sin credenciales; los datos sobreviven a `docker compose stop/up` y el backend se reconecta.
- No probado: el firmware ESP32 (aún no publica por MQTT).
- Limitación conocida: si falla la escritura en la base, el mensaje se pierde (se confirma al broker igualmente; sin reintento).

## Reglas

1. Inspecciona el proyecto antes de modificarlo.
2. Presenta un plan breve y espera aprobación antes de modificar archivos.
3. Reutiliza las dependencias existentes y evita abstracciones innecesarias.
4. No inventes versiones, configuraciones ni requisitos: pregunta o verifica.
5. No ejecutes comandos destructivos (`docker compose down -v`, borrar volúmenes o datos) sin autorización.
6. Distingue entre código escrito, compilación correcta y pruebas realmente ejecutadas, e informa cada estado por separado.

## Agentes y skills

- Agentes (`.claude/agents/`): `backend-architect` (diseña), `spring-boot-developer` (implementa), `mqtt-reviewer` (revisa contratos MQTT).
- Skills (`.claude/skills/`): `mqtt-message-contract`, `database-migration`, `backend-testing`.

## Puesta en marcha local

1. `cp .env.example .env` y completar los valores.
2. Crear el archivo de contraseñas de Mosquitto (una vez):
   `docker run --rm -v "$PWD/docker/mosquitto:/work" eclipse-mosquitto:2.1.2-alpine mosquitto_passwd -b -c /work/passwd <usuario> <contraseña>`
   Luego dar el archivo al usuario del contenedor (Docker lo crea como root con modo 600 y Mosquitto no podría leerlo):
   `docker run --rm -v "$PWD/docker/mosquitto:/work" --entrypoint chown eclipse-mosquitto:2.1.2-alpine mosquitto:mosquitto /work/passwd`
3. `docker compose up -d` / `docker compose stop`. Si el puerto 5432 está ocupado, definir `POSTGRES_PORT` en `.env`.
4. `./mvnw spring-boot:run` (lee `.env`).
