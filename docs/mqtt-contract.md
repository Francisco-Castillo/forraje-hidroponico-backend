# Contrato MQTT: lecturas ambientales

Estado: **backend implementado y probado** (pruebas automatizadas y prueba manual con el Compose local). Firmware: pendiente.

## Tópico

`fvh/<nodo>/lecturas`

- Publica: ESP32. Se suscribe: backend, con `fvh/+/lecturas`.
- `<nodo>`: ID fijo definido en el firmware, formato `[a-z0-9-]`, 1 a 32 caracteres (ej. `esp32-fvh-01`). El `clientId` MQTT del ESP32 es el mismo ID.
- El payload repite el ID en `deviceId`, que **debe coincidir** con `<nodo>`; si no, el mensaje se descarta.

## Entrega

- QoS 1, sin retain.
- QoS 1 puede entregar duplicados. Clave de unicidad: `messageId`. Un duplicado se ignora sin error.
  La base la implementa como (`message_id`, `ts`) porque TimescaleDB exige que las claves únicas de una hypertable incluyan el tiempo; un reenvío lleva el mismo `timestamp`, así que equivale a `messageId` único.

## Payload (JSON, UTF-8, una lectura completa por mensaje)

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| `deviceId` | texto | sí | Igual a `<nodo>` del tópico. |
| `messageId` | texto | sí | No vacío, hasta 64 caracteres. Único por lectura (lo genera el ESP32). |
| `timestamp` | texto ISO-8601 | sí | UTC con sufijo `Z` (admite fracción de segundo). Desde 2024-01-01; una fecha anterior indica reloj sin sincronizar. |
| `measurements` | objeto | sí | Contiene las mediciones siguientes. |
| `measurements.temperatureC` | número, °C | sí | 0 a 50 (rango del DHT11). |
| `measurements.relativeHumidityPct` | número, % HR | sí | 20 a 90 (rango del DHT11). |

Campos desconocidos se ignoran. Un cambio incompatible del contrato requiere un tópico o esquema nuevo; no hay campo de versión.

### Ejemplo válido

```json
{
  "deviceId": "esp32-fvh-01",
  "messageId": "esp32-fvh-01-000123",
  "timestamp": "2026-10-28T20:00:00Z",
  "measurements": {
    "temperatureC": 27.5,
    "relativeHumidityPct": 65.0
  }
}
```

### Ejemplos inválidos (se descartan y se registra el motivo)

- `"temperatureC": "27.5"`: tipo erróneo.
- `"timestamp": "2026-10-28T20:00:00"` o `"...+00:00"`: no es UTC con `Z`.
- `"timestamp": "1970-01-01T00:00:12Z"`: reloj sin sincronizar.
- `"temperatureC": 80`: fuera de rango.
- `deviceId` distinto del tópico, `messageId` vacío o `measurements` ausente.

## Casos de error del backend

JSON malformado, campo ausente o nulo, tipo erróneo, valor fuera de rango, `deviceId` distinto del tópico o `<nodo>` con formato inválido: el mensaje se descarta, se registra el motivo y no se persiste nada.

## Pendiente

- El firmware `DHT11` aún no tiene Wi-Fi, MQTT ni sincronización horaria (NTP). El contrato exige un `timestamp` válido y un `messageId` único por lectura, que debe generar el firmware.
- Un fallo de lectura del DHT11 no publica mensaje (el contrato no define valores nulos).
- Autorización por nodo en Mosquitto (ACL por tópico): no definida; hoy hay un solo usuario por cliente.
