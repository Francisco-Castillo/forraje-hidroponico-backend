package com.fvh.sensores.lectura.api.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Medición almacenada. Los nombres coinciden con el contrato MQTT.")
public record LecturaResponse(
		@Schema(example = "esp32-fvh-01") String deviceId,
		@Schema(example = "esp32-fvh-01-1a2b-000123") String messageId,
		@Schema(description = "Instante de la medición, UTC", example = "2026-10-28T20:00:00Z") Instant timestamp,
		@Schema(description = "Temperatura en °C", example = "27.5") double temperatureC,
		@Schema(description = "Humedad relativa en %", example = "65.0") double relativeHumidityPct) {
}
