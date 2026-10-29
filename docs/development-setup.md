# Puesta en marcha del entorno local

Los comandos marcados **Verificado** se ejecutaron en este proyecto (Linux, Docker Compose v5). Los marcados **Ejemplo** no se ejecutaron.

## 1. Requisitos

- Java 21 (el `pom.xml` fija `java.version` en 21). Maven no hace falta: se usa el wrapper `./mvnw`.
- Docker con Compose v2 o superior.
- Puertos libres en el host: 1883 (MQTT) y 5432 para TimescaleDB, o el que definas en `POSTGRES_PORT`.

## 2. Variables de entorno (`.env`)

**Verificado**

```bash
cp .env.example .env
```

Edita `.env` (no se versiona) y completa:

| Variable | Uso |
|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Base de datos que crea TimescaleDB y a la que se conecta el backend. |
| `POSTGRES_PORT` | Puerto del host para TimescaleDB (por defecto 5432). Cámbialo si está ocupado. |
| `MQTT_USER`, `MQTT_PASSWORD` | Credenciales del backend en Mosquitto. |

Formato `CLAVE=valor`, sin comillas ni espacios. Compose y el backend leen este mismo archivo.

## 3. Archivo de contraseñas de Mosquitto

El broker rechaza conexiones sin credenciales y necesita `docker/mosquitto/passwd` (no se versiona). Debe contener el usuario y la contraseña de `MQTT_USER` y `MQTT_PASSWORD`.

**Verificado** (carga `.env` en el shell y genera el archivo sin escribir la contraseña a mano):

```bash
set -a; . ./.env; set +a
docker run --rm -v "$PWD/docker/mosquitto:/work" eclipse-mosquitto:2.1.2-alpine \
  mosquitto_passwd -b -c /work/passwd "$MQTT_USER" "$MQTT_PASSWORD"
docker run --rm -v "$PWD/docker/mosquitto:/work" --entrypoint chown \
  eclipse-mosquitto:2.1.2-alpine mosquitto:mosquitto /work/passwd
```

El primer comando crea el archivo cifrado; Docker lo deja como root con modo 600. El segundo se lo entrega al usuario del contenedor; sin él Mosquitto no arranca ([troubleshooting](troubleshooting.md#mosquitto-se-reinicia-en-bucle)).

## 4. Servicios (Docker Compose)

**Verificado**

| Comando | Qué hace |
|---|---|
| `docker compose up -d` | Crea y arranca `timescaledb` y `mosquitto` en segundo plano. |
| `docker compose ps` | Lista los contenedores. `timescaledb` debe indicar `healthy` y `mosquitto` `Up`. |
| `docker compose logs -f mosquitto` | Sigue los logs de un servicio (`timescaledb` igual). Con éxito, Mosquitto termina con `mosquitto version 2.1.2 running`. |
| `docker compose stop` | Detiene los servicios y conserva contenedores y datos. |
| `docker compose up -d` | (de nuevo) Vuelve a arrancarlos con los datos anteriores. |

Los datos viven en los volúmenes `sensores_timescaledb-data` y `sensores_mosquitto-data`. `stop`, `restart` y `down` los conservan.

> **No ejecutes `docker compose down -v`**: borra los volúmenes y todos los datos.

Los servicios son independientes del proceso Spring Boot: se pueden levantar y detener por separado.

## 5. Backend

**Desde la terminal** (Verificado). Debe ejecutarse en la raíz del proyecto, porque ahí lee `.env`:

```bash
./mvnw spring-boot:run
```

Está listo cuando el log muestra `Started SensoresApplication`. Detenlo con `Ctrl+C`. Al arrancar, Flyway aplica las migraciones pendientes.

**Desde VS Code** (Ejemplo, no verificado). Con la extensión Spring Boot Extension Pack, abre `SensoresApplication` y usa *Run* sobre `main`, o el panel *Spring Boot Dashboard*. El directorio de trabajo debe ser la raíz del proyecto, para que se lea `.env`.

**Desde Eclipse** (Ejemplo, no verificado). Clic derecho sobre el proyecto, *Run As > Spring Boot App* (con Spring Tools) o *Java Application* sobre `SensoresApplication`. Por defecto el directorio de trabajo es la raíz del proyecto.

## 6. Verificación de extremo a extremo

Con los servicios y el backend en marcha. **Verificado**.

1. Publicar un mensaje de prueba (usa las credenciales de `.env`):

   ```bash
   set -a; . ./.env; set +a
   docker compose exec -T mosquitto mosquitto_pub -h localhost \
     -u "$MQTT_USER" -P "$MQTT_PASSWORD" -q 1 -t fvh/esp32-fvh-01/lecturas \
     -m "{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"prueba-$(date +%s)\",\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65.0}}"
   ```

   Publica una lectura válida con `messageId` único y la hora actual UTC.

2. Comprobar la recepción. Un mensaje válido **no** genera log. Un mensaje descartado sí: el log del backend muestra `Mensaje descartado (<motivo>)`. La confirmación de un mensaje válido es la fila en la base.

3. Comprobar la persistencia:

   ```bash
   docker compose exec -T timescaledb sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
     -c "SELECT message_id, nodo, ts, temperatura_c, humedad_pct FROM lecturas ORDER BY ts DESC LIMIT 5;"'
   ```

   Ejecuta `psql` dentro del contenedor, que ya conoce `POSTGRES_USER` y `POSTGRES_DB`. Repetir el paso 1 con el mismo `messageId` y `timestamp` no añade otra fila (duplicado ignorado).

4. Para ver que el broker rechaza conexiones anónimas, ejecuta el paso 1 sin `-u` ni `-P`: responde `The connection was refused`.

5. Consultar por la API REST (puerto 8080; **Verificado** el 2026-10-29 contra la base del Compose, con resultados iguales a los del `SELECT`):

   ```bash
   curl -s localhost:8080/api/v1/dispositivos/esp32-fvh-01/lecturas/ultima
   curl -s "localhost:8080/api/v1/dispositivos/esp32-fvh-01/lecturas?desde=2026-10-29T17:30:00Z&hasta=2026-10-29T17:40:00Z&size=5&sort=ts,desc"
   ```

   Ajusta las fechas a un intervalo con datos (UTC). Swagger UI: <http://localhost:8080/swagger-ui.html>; especificación: `/v3/api-docs`. La API no tiene autenticación: no la expongas fuera de la red local. Detalle en [api.md](api.md).

## 7. Pruebas

Ver [testing.md](testing.md).
