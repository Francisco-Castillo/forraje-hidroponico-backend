---
name: update-documentation
description: Procedimiento para identificar y actualizar solo la documentación afectada por un cambio de código, configuración, esquema, mensajes MQTT o pruebas. Usar antes de dar por terminada cualquier tarea que cambie el sistema.
---

# Actualización de documentación

1. **Resume el cambio.** Anota qué cambió y su estado: implementado, verificado (compila) o probado (con qué comando y resultado reales).
2. **Localiza lo afectado.** Guíate por esta tabla y busca con `grep` los nombres modificados en `README.md`, `docs/` y `CLAUDE.md`:

   | Si cambia... | Revisa |
   |---|---|
   | Componentes, flujo o límites | `docs/architecture.md` |
   | Variables, puertos, `docker-compose.yml`, `.env.example`, comandos de arranque | `docs/development-setup.md`, `docs/troubleshooting.md` |
   | Tópico, campos, unidades, QoS, validación | `docs/mqtt-contract.md` (y `docs/testing.md` si cambian las pruebas) |
   | Migraciones, tablas, consultas | `docs/database.md` |
   | Pruebas o su resultado | `docs/testing.md` |
   | Funcionalidad nueva o estado general | `README.md` (tabla de estado) |
   | Cualquier cambio técnico relevante | `docs/changes.md` (entrada breve, con el hash del commit si existe) |
   | Reglas de trabajo, agentes, skills | `CLAUDE.md` |

3. **Actualiza solo eso.** Modifica los pasajes afectados; no reescribas documentos enteros. Si un dato ya vive en otro documento, enlázalo.
4. **Respeta los estados.** No marques como verificado ni probado lo que no ejecutaste. Lo desconocido sigue como **desconocido**; las funcionalidades no implementadas no se describen como disponibles.
5. **Comprueba contra la implementación.** Los comandos, rutas, puertos, tópicos, variables y ejemplos citados deben coincidir con `src/`, `docker-compose.yml`, `application.yaml` y `.env.example`. Si cambiaste un comando documentado, vuelve a ejecutarlo; si no puedes, márcalo como **Ejemplo**.
6. **Comprueba los enlaces.** Verifica que los archivos referenciados y los enlaces relativos de los `.md` existan.
7. **Informa.** Lista los documentos actualizados y los datos pendientes. Si no hacía falta tocar documentación, explica por qué.

## Reglas

- Sin credenciales reales ni secretos en la documentación: usa marcadores como `<usuario>`.
- No introduzcas funcionalidades ni cambios de código para justificar la documentación.
