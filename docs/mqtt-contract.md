# Contrato MQTT: lecturas ambientales

Estado: **backend implementado y probado** (pruebas automatizadas con TimescaleDB y Mosquitto reales). Firmware: pendiente.

## Tópico

`fvh/<nodo>/lecturas`

- Publica: ESP32. Se suscribe: backend, con `fvh/+/lecturas`.
- `<nodo>`: ID fijo definido en el firmware, formato `[a-z0-9-]`, 1 a 32 caracteres (ej. `bandeja-01`). Es la única fuente de identidad del nodo: no se repite en el payload.
- El `clientId` MQTT del ESP32 es el mismo `<nodo>`.

## Entrega

- QoS 1, sin retain.
- QoS 1 puede entregar duplicados. Clave de unicidad: (`<nodo>`, `ts`). Un duplicado se ignora sin error.

## Payload (JSON, UTF-8, una lectura completa por mensaje)

| Campo | Tipo | Unidad | Obligatorio | Validación |
|---|---|---|---|---|
| `v` | entero | - | sí | Debe ser `1`. |
| `ts` | entero | segundos epoch UTC, medido por el ESP32 | sí | >= 1704067200 (2024-01-01); un valor menor indica reloj sin sincronizar. |
| `temperatura_c` | número | °C | sí | 0 a 50 (rango del DHT11). |
| `humedad_pct` | número | % HR | sí | 20 a 90 (rango del DHT11). |

Campos desconocidos: se ignoran. Los campos nuevos que se añadan sin romper a los existentes mantienen `v` = 1; un cambio incompatible sube `v`.

### Ejemplo válido

```json
{"v":1,"ts":1761694800,"temperatura_c":24.5,"humedad_pct":61.0}
```

### Ejemplos inválidos (se descartan y se registra el motivo)

```json
{"v":1,"ts":1761694800,"temperatura_c":"24.5","humedad_pct":61.0}
{"v":1,"ts":12,"temperatura_c":24.5,"humedad_pct":61.0}
{"v":1,"ts":1761694800,"temperatura_c":80,"humedad_pct":61.0}
{"v":1,"ts":1761694800,"humedad_pct":61.0}
```

Motivos respectivos: tipo erróneo, `ts` de reloj sin sincronizar, temperatura fuera de rango, campo ausente.

## Casos de error del backend

JSON malformado, campo ausente, tipo erróneo, valor fuera de rango, `<nodo>` con formato inválido o `v` desconocida: el mensaje se descarta, se registra el motivo y no se persiste nada.

## Pendiente

- El firmware `DHT11` aún no tiene Wi-Fi, MQTT ni sincronización horaria (NTP). El contrato exige `ts` válido, así que esa sincronización es requisito del firmware.
- Un fallo de lectura del DHT11 no publica mensaje (el contrato no define valores nulos).
- Autorización por nodo en Mosquitto (ACL por tópico): no definida; hoy hay un solo usuario por cliente.
