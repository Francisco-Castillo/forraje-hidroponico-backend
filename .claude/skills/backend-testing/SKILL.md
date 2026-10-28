---
name: backend-testing
description: Procedimiento para definir y ejecutar pruebas unitarias y de integración del backend. Usar tras implementar o cambiar una funcionalidad, y antes de dar algo por funcionando.
---

# Pruebas del backend

Se ejecutan con `./mvnw test`.

1. **Alcance.** Lista las funcionalidades cambiadas y qué comportamiento debe verificarse.
2. **Elegir el nivel.**
   - Unitaria: validación de mensajes, parseo, reglas. Sin Spring ni infraestructura.
   - Integración: persistencia con `JdbcTemplate`, migraciones y recepción MQTT, contra una TimescaleDB y un broker reales (no simulados) cuando lo que importa es esa integración.
3. **Casos.** Incluye siempre el caso válido, los inválidos (JSON malformado, campos ausentes, tipos o rangos erróneos) y los duplicados, según el contrato vigente.
4. **Infraestructura.** Las pruebas de integración no deben depender de datos de desarrollo ni borrar volúmenes. Añadir una dependencia nueva (p. ej. Testcontainers) requiere aprobación del usuario.
5. **Ejecución.** Ejecuta las pruebas y reporta por separado: código escrito, compilación (`./mvnw compile`) y pruebas ejecutadas con su resultado (pasadas, falladas, omitidas).

## Reglas

- No declares que algo funciona si su prueba no se ejecutó; indica qué quedó sin probar y por qué.
- Si una prueba falla, muestra el error real; no la modifiques para que pase sin explicar el motivo.
