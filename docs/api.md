# API REST de consulta

Solo lectura. **Sin autenticación**: no exponerla fuera de la red local. Base: `http://localhost:8080`. Swagger UI: `/swagger-ui.html`; especificación OpenAPI: `/v3/api-docs`.

Estado: implementada; probada con pruebas automatizadas y con el backend real contra la base del Compose (2026-10-29).

## Endpoints

### `GET /api/v1/dispositivos/{deviceId}/lecturas/ultima`

Última medición del dispositivo. `404` si no tiene mediciones; `400` si `deviceId` no cumple `[a-z0-9-]{1,32}`.

### `GET /api/v1/dispositivos/{deviceId}/lecturas`

Historial paginado.

| Parámetro | Obligatorio | Regla |
|---|---|---|
| `desde` | sí | ISO-8601 UTC (`2026-10-28T00:00:00Z`), inclusivo. |
| `hasta` | sí | ISO-8601 UTC, exclusivo. Debe ser posterior a `desde`; el intervalo no puede superar 7 días (`sensores.api.max-rango`). |
| `temperaturaMin`, `temperaturaMax` | no | °C, finitos, mín ≤ máx. |
| `humedadMin`, `humedadMax` | no | % HR, finitos, mín ≤ máx. |
| `page` | no (0) | ≥ 0. |
| `size` | no (20) | 1 a 100 (`sensores.api.max-page-size`). |
| `sort` | no (`ts,desc`) | `campo[,asc\|desc]` con campo en `ts`, `temperatureC`, `relativeHumidityPct`. Cualquier otro se rechaza. Se desempata por `ts` descendente. |

## Respuesta

Medición (mismos nombres que el [contrato MQTT](mqtt-contract.md)):

```json
{"deviceId":"esp32-fvh-01","messageId":"esp32-fvh-01-603e47dc-219","timestamp":"2026-10-29T17:37:25Z","temperatureC":27.1,"relativeHumidityPct":53.0}
```

Historial: `{"content":[...],"page":0,"size":2,"totalElements":146,"totalPages":73}`.

## Errores

`application/problem+json` (RFC 9457): `400` para parámetros faltantes, de tipo erróneo o que incumplen las reglas; `404` si no hay mediciones. El mensaje (`detail`) no incluye detalles internos.

## Ejemplos

```bash
curl -s localhost:8080/api/v1/dispositivos/esp32-fvh-01/lecturas/ultima
curl -s "localhost:8080/api/v1/dispositivos/esp32-fvh-01/lecturas?desde=2026-10-29T17:30:00Z&hasta=2026-10-29T17:40:00Z&temperaturaMin=27&size=2&sort=temperatureC,desc"
```

## Notas

- Los datos son los de la tabla `lecturas` ([database.md](database.md)); la API no escribe.
- Cada consulta del historial ejecuta además un `COUNT` para los totales de página.
