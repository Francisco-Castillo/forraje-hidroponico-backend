---
name: mqtt-message-contract
description: Procedimiento para diseñar, modificar y validar los contratos de mensajes MQTT entre el ESP32 y el backend. Usar al definir o cambiar tópicos, campos o QoS.
---

# Contrato de mensajes MQTT

Decisiones vigentes: JSON, una lectura completa por mensaje, timestamp del ESP32, QoS 1 (ver `CLAUDE.md`). El contrato concreto se documenta en `docs/mqtt-contract.md`.

1. **Alcance.** Indica qué tópico o mensaje se define o cambia y si afecta al firmware, al backend o a ambos.
2. **Tópico.** Define el patrón, qué identifica al nodo y quién publica o se suscribe.
3. **Payload.** Para cada campo indica nombre, tipo JSON, unidad, rango válido y si es obligatorio. Fija el formato del timestamp y de la versión del esquema si existe.
4. **QoS y duplicados.** QoS 1 puede entregar duplicados: define la clave que los identifica (por ejemplo nodo + timestamp) y cómo se tratan.
5. **Casos inválidos.** Define qué hace el backend con JSON malformado, campos ausentes, tipos erróneos o valores fuera de rango (descartar y registrar).
6. **Ejemplo.** Incluye un mensaje válido y uno inválido en `docs/mqtt-contract.md`.
7. **Compatibilidad.** Si cambia un contrato existente, indica qué debe cambiar en firmware y backend; consulta a `mqtt-reviewer`.

## Reglas

- No inventes campos, unidades ni rangos: si faltan datos del sensor, regístralos como pendientes.
- El contrato se escribe en un único lugar; el código y los tests lo referencian.
