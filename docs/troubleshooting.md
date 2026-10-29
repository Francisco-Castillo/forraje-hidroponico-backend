# Problemas frecuentes

Solo se listan problemas encontrados en este proyecto.

## Mosquitto se reinicia en bucle

`docker compose ps` muestra `Restarting` y el log dice `Unable to open pwfile "/mosquitto/config/passwd"`.

- **Causa:** `docker/mosquitto/passwd` no existe o lo creó Docker como root con modo 600, y Mosquitto corre como usuario `mosquitto`.
- **Solución:** crear el archivo y cambiar su propietario (ver [development-setup.md](development-setup.md#3-archivo-de-contraseñas-de-mosquitto)); luego `docker compose restart mosquitto`.

## El puerto 5432 está ocupado

`docker compose up -d` falla al publicar el puerto de `timescaledb`.

- **Causa:** otro PostgreSQL (por ejemplo otro contenedor) usa el 5432.
- **Solución:** definir `POSTGRES_PORT=5433` (u otro libre) en `.env`. Compose y el backend lo respetan. No detengas el otro servicio sin saber de qué es.

## El backend no arranca

- Mensaje sobre un placeholder `${POSTGRES_DB}` o `${MQTT_USER}` sin resolver: falta `.env` o se ejecuta fuera de la raíz del proyecto. Compruébalo con `ls .env` y ejecuta desde la raíz.
- Fallo de conexión a la base: comprueba que `docker compose ps` muestra `timescaledb` como `healthy` y que `POSTGRES_PORT` es el mismo que usa Compose.

## El backend no se conecta al broker

El log muestra `Lost connection` o errores de conexión de Paho.

- Comprueba que `mosquitto` está `Up` y que `MQTT_USER`/`MQTT_PASSWORD` de `.env` son los mismos con que se creó `passwd`.
- Si cambias la contraseña en `.env`, regenera `passwd` y reinicia Mosquitto.
- El cliente reconecta solo cuando el broker vuelve a estar disponible (probado reiniciando los servicios).

## Publiqué un mensaje y no aparece en la base

1. Busca en el log del backend `Mensaje descartado (...)`: indica el motivo exacto (formato, rango, `deviceId` distinto del tópico, etc.). Revisa el [contrato](mqtt-contract.md).
2. Si no hay descarte, comprueba que el tópico es `fvh/<nodo>/lecturas` y que usaste el mismo usuario y contraseña del broker (sin credenciales `mosquitto_pub` responde `The connection was refused`).
3. Un `messageId` repetido con el mismo `timestamp` es un duplicado: se ignora sin error y sin log (nivel DEBUG).
4. Un mensaje válido no genera log; verifica directamente en la base ([database.md](database.md#consultas-útiles-verificado)).

## Las pruebas de integración fallan

- Docker debe estar en marcha y el usuario debe poder usarlo.
- La primera ejecución descarga las imágenes `timescale/timescaledb:2.31.0-pg17` y `eclipse-mosquitto:2.1.2-alpine`.
