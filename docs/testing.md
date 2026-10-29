# Pruebas

Comandos **Verificados**. Los resultados indicados son los de la última ejecución registrada en [changes.md](changes.md); vuelve a ejecutar para confirmarlos.

| Comando | Qué hace |
|---|---|
| `./mvnw compile` | Compila el código principal. |
| `./mvnw test` | Compila y ejecuta todas las pruebas. |
| `./mvnw test -Dtest=LecturaParserTest` | Solo las pruebas unitarias del parser (no necesitan Docker). |

## Qué hay

| Clase | Tipo | Cubre |
|---|---|---|
| `LecturaParserTest` | Unitaria (44 casos) | Mensaje válido, campos desconocidos, `soilMoistureRaw` (rango, ausente, nulo e inválido), límites de rango, y los inválidos: JSON malformado, campos ausentes o nulos, tipos erróneos, `timestamp` sin `Z`, con desfase o de reloj sin sincronizar, `deviceId` distinto del tópico, `messageId` vacío o largo, valores fuera de rango y tópicos inválidos. |
| `LecturaConsultaServiceImplTest` | Unitaria (12 casos) | Validación de `deviceId`, intervalo (inclusive el límite exacto de 7 días), tamaño y número de página, límites de valores y orden (lista blanca, traducción de nombres, rechazo de campos no permitidos). |
| `LecturaControllerTest` | `@WebMvcTest` (8 casos) | Formato JSON (incluido `soilMoistureRaw`), 404/400 como `ProblemDetail`, parámetros obligatorios, tipos erróneos, `deviceId` inválido y valores por defecto, con el servicio simulado. |
| `LecturaApiIntegrationTest` | Integración (10 casos) | HTTP contra TimescaleDB real: última medición, `soilMoistureRaw` (con valor y nulo), intervalo `[desde, hasta)`, filtros, orden, paginación, validaciones y el índice `V3`. |
| `LecturasIntegrationTest` | Integración (4 casos) | Contenedores reales de TimescaleDB y Mosquitto con Testcontainers: la migración crea la hypertable, un mensaje publicado por MQTT llega a la tabla, `soilMoistureRaw` se guarda (o queda nulo si falta) y uno fuera de rango se descarta, y los duplicados y mensajes inválidos no se guardan. |

Las pruebas de integración **requieren Docker en marcha** y descargan las imágenes la primera vez. Usan contenedores propios: no tocan tus servicios ni sus volúmenes, y el broker de pruebas no exige autenticación.

## Resultado de la última ejecución

78 pruebas (44 parser + 4 ingesta + 30 de la API), 0 fallos (2026-10-29, tras `./mvnw clean test`). Al final Surefire avisa `Surefire is going to kill self fork JVM`: la JVM tarda más de 30 s en cerrarse; no falla ninguna prueba y la causa no está investigada.

## Prueba física de extremo a extremo (2026-10-29)

Manual, con hardware real: ESP32 (`esp32dev`) con DHT11 en GPIO4 conectado por USB, Wi-Fi 2,4 GHz, Mosquitto y TimescaleDB del Compose local, y el firmware del proyecto `DHT11` con `include/config.h` rellenado (`MQTT_HOST` = IP del PC en la red).

| Paso | Resultado |
|---|---|
| Monitor serie del ESP32 | Muestra lecturas de temperatura y humedad (confirmado por el usuario). |
| `mosquitto_sub -t 'fvh/#'` dentro del contenedor, con las credenciales de `.env`, con el backend **apagado** | Llegan mensajes a `fvh/esp32-fvh-01/lecturas`, uno cada ~2 s, con el formato del [contrato](mqtt-contract.md). |
| Arranque del backend (`./mvnw spring-boot:run`) | `Started SensoresApplication in 5.7 seconds`. |
| `SELECT ... FROM lecturas ORDER BY ts DESC` | Filas de `esp32-fvh-01` con `ts` de la hora actual UTC y `message_id` del tipo `esp32-fvh-01-<bootId>-<n>`, aproximadamente 27 °C y 52 %. |

Conclusiones:

- El camino feliz funciona: ESP32, Wi-Fi, NTP, autenticación MQTT, publicación, backend y TimescaleDB.
- Los mensajes publicados mientras el backend estaba apagado **no se guardaron**. Con QoS 0 el broker no los retiene para el backend. Comportamiento esperado, no un fallo nuevo.
- Las filas se pueden consultar desde DBeaver ([database.md](database.md#acceso-desde-dbeaver-verificado)).

**No probado todavía** en hardware: Wi-Fi caído, broker detenido, sensor desconectado y clave MQTT errónea; reconexión automática del firmware y del backend; publicación con QoS 1 (el firmware usa QoS 0); ejecución prolongada.

## No cubierto

- Reconexión del cliente MQTT y reenvío de mensajes QoS 1 tras una caída del backend.
- Autenticación contra el Mosquitto del Compose (las pruebas automatizadas usan acceso anónimo; la autenticación se comprobó a mano, ver [development-setup.md](development-setup.md#6-verificación-de-extremo-a-extremo)).
- Los casos de fallo del firmware ESP32 (el camino feliz solo se probó a mano, ver la sección anterior).
