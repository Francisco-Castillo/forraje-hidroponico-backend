# Arquitectura

## Flujo de datos

```
ESP32 --MQTT QoS 1--> Mosquitto --> backend Spring Boot --JDBC--> TimescaleDB
        (fvh/<nodo>/lecturas)       (suscrito a fvh/+/lecturas)    (tabla lecturas)
```

Consulta (solo lectura): `cliente HTTP --> LecturaController --> LecturaConsultaService --> LecturaJpaRepository (Specification) --> TimescaleDB`. Ver [api.md](api.md).

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
| `lectura.api.LecturaController` / `LecturaControllerImpl` | `.../lectura/api/` | API REST de consulta. La interfaz lleva el mapeo HTTP, la validación de parámetros y las anotaciones OpenAPI. `LecturaMapper` (MapStruct) convierte entidad a DTO; `ApiExceptionHandler` responde errores como `ProblemDetail`. |
| `lectura.consulta.LecturaConsultaService` / `Impl` | `.../lectura/consulta/` | Reglas de consulta: valida `deviceId`, intervalo (máx. 7 días), tamaño de página y orden (lista blanca), y compone las `Specification` (`LecturaSpecifications`). Límites en `sensores.api.*` (`ConsultaProperties`). |
| `lectura.consulta.LecturaEntity` / `LecturaJpaRepository` | `.../lectura/consulta/` | Entidad JPA **inmutable** sobre `lecturas` (clave `LecturaId` = `message_id`, `ts`) y repositorio con `JpaSpecificationExecutor`. Solo lectura. |
| Flyway | `src/main/resources/db/migration/` | Migraciones del esquema; ver [database.md](database.md). |

## Decisiones

- Una lectura completa por mensaje, JSON, timestamp generado por el ESP32, QoS 1.
- Con QoS 1 pueden llegar duplicados: se ignoran por `messageId` (ver [contrato](mqtt-contract.md#entrega)).
- Persistencia: la **escritura** (ingesta MQTT) sigue con `JdbcTemplate`; la **lectura** (API) usa JPA con una entidad inmutable y `Specification`. Dos accesos a la misma tabla, a propósito: no se tocó la ingesta que ya funcionaba. `ddl-auto=validate`: Hibernate nunca modifica el esquema (lo hace Flyway).
- Controladores y servicios de la API se definen como interfaz + implementación.

## Limitaciones conocidas

- Si falla la escritura en la base, el mensaje se pierde: se confirma al broker igualmente y no hay reintento.
- Un mensaje válido no genera log; solo se registran los descartados (ver [troubleshooting](troubleshooting.md)).
- Una autorización por nodo en Mosquitto (ACL) no está definida; hay un solo usuario para el backend.
- La API de consulta no tiene autenticación (Swagger UI incluido); no exponerla fuera de la red local.
- El historial hace un `COUNT` por consulta; acotado por el máximo de 7 días.
- Dependencias presentes pero sin uso: `spring-integration-jpa`, `spring-integration-http`.
