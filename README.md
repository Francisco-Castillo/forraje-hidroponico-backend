# Backend sensores: forraje verde hidropónico

Backend Spring Boot que recibe por MQTT las mediciones ambientales de un ESP32, las valida y las guarda en TimescaleDB.

## Estado

| Funcionalidad | Estado |
|---|---|
| Recepción MQTT (`fvh/+/lecturas`, QoS 1, con usuario y contraseña) | Implementada y probada (pruebas automatizadas y prueba manual con el Compose local) |
| Validación según [el contrato](docs/mqtt-contract.md) | Implementada y probada |
| Persistencia en TimescaleDB, con deduplicación | Implementada y probada |
| Firmware ESP32 que publica por MQTT | Implementado y compilado, sin probar (proyecto `DHT11`; QoS 0) |
| Actuadores, reglas de automatización, API de consulta histórica | No implementados |

## Arranque rápido

Requisitos: Java 21, Docker con Compose. El detalle está en [docs/development-setup.md](docs/development-setup.md).

```bash
cp .env.example .env          # completar los valores
# crear docker/mosquitto/passwd (ver development-setup.md)
docker compose up -d          # TimescaleDB y Mosquitto
./mvnw spring-boot:run        # backend
```

## Documentación

| Documento | Contenido |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Componentes y flujo de datos |
| [docs/development-setup.md](docs/development-setup.md) | Entorno local, servicios, ejecución y verificación de extremo a extremo |
| [docs/mqtt-contract.md](docs/mqtt-contract.md) | Tópicos, mensajes, campos, unidades, QoS y ejemplos |
| [docs/database.md](docs/database.md) | Esquema, migraciones y consultas útiles |
| [docs/testing.md](docs/testing.md) | Compilación y pruebas |
| [docs/troubleshooting.md](docs/troubleshooting.md) | Errores frecuentes |
| [docs/changes.md](docs/changes.md) | Historial de cambios técnicos |
| [CLAUDE.md](CLAUDE.md) | Reglas de trabajo con Claude Code, agentes y skills |
