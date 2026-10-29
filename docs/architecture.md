# Arquitectura

## Flujo de datos

```
ESP32 --MQTT QoS 1--> Mosquitto --> backend Spring Boot --JDBC--> TimescaleDB
        (fvh/<nodo>/lecturas)       (suscrito a fvh/+/lecturas)    (tabla lecturas)
```

El ESP32 aún no publica (ver [README](../README.md#estado)); hoy los mensajes se simulan con `mosquitto_pub`.

## Componentes

| Componente | Dónde | Responsabilidad |
|---|---|---|
| Mosquitto | `docker-compose.yml`, `docker/mosquitto/` | Broker MQTT con usuario y contraseña. Conserva la sesión del backend (mensajes QoS 1 pendientes). |
| TimescaleDB | `docker-compose.yml` | PostgreSQL con TimescaleDB; datos en un volumen de Docker. |
| `mqtt.MqttConfig` | `src/main/java/com/fvh/sensores/mqtt/` | Cliente Paho (Spring Integration): conexión con reconexión automática, sesión persistente, suscripción QoS 1 y envío al servicio. Las propiedades están en `MqttProperties`. |
| `lectura.LecturaParser` | `.../lectura/` | Valida tópico y JSON según [el contrato](mqtt-contract.md). Lanza `LecturaInvalidaException`. |
| `lectura.LecturaService` | `.../lectura/` | Orquesta: parsea, descarta y registra lo inválido, guarda lo válido. |
| `lectura.LecturaRepository` | `.../lectura/` | `INSERT ... ON CONFLICT DO NOTHING` con `JdbcTemplate`. |
| Flyway | `src/main/resources/db/migration/` | Migraciones del esquema; ver [database.md](database.md). |

## Decisiones

- Una lectura completa por mensaje, JSON, timestamp generado por el ESP32, QoS 1.
- Con QoS 1 pueden llegar duplicados: se ignoran por `messageId` (ver [contrato](mqtt-contract.md#entrega)).
- Persistencia con `JdbcTemplate`, sin JPA.

## Limitaciones conocidas

- Si falla la escritura en la base, el mensaje se pierde: se confirma al broker igualmente y no hay reintento.
- Un mensaje válido no genera log; solo se registran los descartados (ver [troubleshooting](troubleshooting.md)).
- Una autorización por nodo en Mosquitto (ACL) no está definida; hay un solo usuario para el backend.
- Dependencias presentes pero sin uso: `spring-boot-starter-data-jpa`, `spring-integration-jpa`, `spring-integration-http`, `spring-boot-starter-webmvc`.
