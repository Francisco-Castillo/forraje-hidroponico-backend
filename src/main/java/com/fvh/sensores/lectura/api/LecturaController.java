package com.fvh.sensores.lectura.api;

import java.time.Instant;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fvh.sensores.lectura.api.dto.LecturaResponse;
import com.fvh.sensores.lectura.api.dto.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

@Tag(name = "Lecturas", description = "Consulta de mediciones almacenadas (solo lectura). Sin autenticación: no exponer fuera de la red local.")
@RequestMapping(value = "/api/v1/dispositivos/{deviceId}/lecturas", produces = MediaType.APPLICATION_JSON_VALUE)
public interface LecturaController {

	String DEVICE_ID_REGEX = "^[a-z0-9-]{1,32}$";

	@Operation(summary = "Última medición de un dispositivo")
	@ApiResponse(responseCode = "200", description = "Medición más reciente")
	@ApiResponse(responseCode = "400", description = "deviceId inválido", content = @Content(mediaType = "application/problem+json"))
	@ApiResponse(responseCode = "404", description = "El dispositivo no tiene mediciones", content = @Content(mediaType = "application/problem+json"))
	@GetMapping("/ultima")
	LecturaResponse ultima(
			@Parameter(description = "Identificador del dispositivo", example = "esp32-fvh-01") @PathVariable @Pattern(regexp = DEVICE_ID_REGEX) String deviceId);

	@Operation(summary = "Historial de mediciones en un intervalo",
			description = "Intervalo [desde, hasta) en UTC, de hasta 7 días (configurable). Paginado y ordenable; filtros opcionales por rango de valores.")
	@ApiResponse(responseCode = "200", description = "Página de mediciones")
	@ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content(mediaType = "application/problem+json"))
	@GetMapping
	PaginaResponse<LecturaResponse> historial(
			@Parameter(description = "Identificador del dispositivo", example = "esp32-fvh-01") @PathVariable @Pattern(regexp = DEVICE_ID_REGEX) String deviceId,
			@Parameter(description = "Inicio inclusivo, ISO-8601 UTC", example = "2026-10-28T00:00:00Z") @RequestParam Instant desde,
			@Parameter(description = "Fin exclusivo, ISO-8601 UTC", example = "2026-10-29T00:00:00Z") @RequestParam Instant hasta,
			@Parameter(description = "Temperatura mínima (°C)") @RequestParam(required = false) Double temperaturaMin,
			@Parameter(description = "Temperatura máxima (°C)") @RequestParam(required = false) Double temperaturaMax,
			@Parameter(description = "Humedad mínima (%)") @RequestParam(required = false) Double humedadMin,
			@Parameter(description = "Humedad máxima (%)") @RequestParam(required = false) Double humedadMax,
			@Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(0) int page,
			@Parameter(description = "Tamaño de página (máximo configurable, 100 por defecto)") @RequestParam(defaultValue = "20") @Min(1) int size,
			@Parameter(description = "Orden: campo[,asc|desc]; campos: ts, temperatureC, relativeHumidityPct", example = "ts,desc") @RequestParam(required = false) String sort);
}
