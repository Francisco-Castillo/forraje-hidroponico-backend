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
| `LecturaParserTest` | Unitaria (34 casos) | Mensaje válido, campos desconocidos, límites de rango, y los inválidos: JSON malformado, campos ausentes o nulos, tipos erróneos, `timestamp` sin `Z`, con desfase o de reloj sin sincronizar, `deviceId` distinto del tópico, `messageId` vacío o largo, valores fuera de rango y tópicos inválidos. |
| `LecturasIntegrationTest` | Integración (3 casos) | Contenedores reales de TimescaleDB y Mosquitto con Testcontainers: la migración crea la hypertable, un mensaje publicado por MQTT llega a la tabla, y los duplicados y mensajes inválidos no se guardan. |

Las pruebas de integración **requieren Docker en marcha** y descargan las imágenes la primera vez. Usan contenedores propios: no tocan tus servicios ni sus volúmenes, y el broker de pruebas no exige autenticación.

## Resultado de la última ejecución

37 pruebas, 0 fallos, `BUILD SUCCESS`. Al final Surefire avisa `Surefire is going to kill self fork JVM`: la JVM tarda más de 30 s en cerrarse; no falla ninguna prueba y la causa no está investigada.

## No cubierto

- Reconexión del cliente MQTT y reenvío de mensajes QoS 1 tras una caída del backend.
- Autenticación contra el Mosquitto del Compose (las pruebas automatizadas usan acceso anónimo; la autenticación se comprobó a mano, ver [development-setup.md](development-setup.md#6-verificación-de-extremo-a-extremo)).
- El firmware ESP32.
