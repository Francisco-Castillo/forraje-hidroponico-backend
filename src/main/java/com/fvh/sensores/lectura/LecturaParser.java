package com.fvh.sensores.lectura;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Valida un mensaje según docs/mqtt-contract.md. */
@Component
public class LecturaParser {

	private static final Pattern TOPIC = Pattern.compile("^fvh/([a-z0-9-]{1,32})/lecturas$");
	private static final long TS_MINIMO = 1704067200L; // 2024-01-01: menos indica reloj sin sincronizar

	private final JsonMapper jsonMapper;

	public LecturaParser(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public Lectura parse(String topic, String payload) {
		Matcher m = TOPIC.matcher(topic == null ? "" : topic);
		if (!m.matches()) {
			throw new LecturaInvalidaException("tópico inválido: " + topic);
		}

		JsonNode json;
		try {
			json = jsonMapper.readTree(payload);
		} catch (JacksonException e) {
			throw new LecturaInvalidaException("JSON malformado");
		}
		if (!json.isObject()) {
			throw new LecturaInvalidaException("el payload no es un objeto JSON");
		}

		String deviceId = texto(json, "deviceId");
		if (!deviceId.equals(m.group(1))) {
			throw new LecturaInvalidaException("deviceId '" + deviceId + "' no coincide con el tópico");
		}
		String messageId = texto(json, "messageId");
		if (messageId.isBlank() || messageId.length() > 64) {
			throw new LecturaInvalidaException("messageId vacío o de más de 64 caracteres");
		}
		Instant ts = instante(texto(json, "timestamp"));

		JsonNode mediciones = campo(json, "measurements");
		if (!mediciones.isObject()) {
			throw new LecturaInvalidaException("measurements no es un objeto");
		}
		double temperatura = numero(mediciones, "temperatureC", 0, 50);
		double humedad = numero(mediciones, "relativeHumidityPct", 20, 90);

		return new Lectura(deviceId, messageId, ts, temperatura, humedad);
	}

	private static String texto(JsonNode json, String nombre) {
		JsonNode valor = campo(json, nombre);
		if (!valor.isString()) {
			throw new LecturaInvalidaException(nombre + " no es texto: " + valor);
		}
		return valor.stringValue();
	}

	private static Instant instante(String valor) {
		// Solo UTC con sufijo Z (Instant.parse también aceptaría otros desfases).
		if (!valor.endsWith("Z")) {
			throw new LecturaInvalidaException("timestamp no está en UTC (sufijo Z): " + valor);
		}
		Instant ts;
		try {
			ts = Instant.parse(valor);
		} catch (DateTimeParseException e) {
			throw new LecturaInvalidaException("timestamp no es ISO-8601: " + valor);
		}
		if (ts.getEpochSecond() < TS_MINIMO) {
			throw new LecturaInvalidaException("timestamp anterior a 2024-01-01 (reloj sin sincronizar): " + valor);
		}
		return ts;
	}

	private static JsonNode campo(JsonNode json, String nombre) {
		JsonNode valor = json.get(nombre);
		if (valor == null || valor.isNull()) {
			throw new LecturaInvalidaException("falta el campo " + nombre);
		}
		return valor;
	}

	private static double numero(JsonNode json, String nombre, double min, double max) {
		JsonNode valor = campo(json, nombre);
		if (!valor.isNumber()) {
			throw new LecturaInvalidaException(nombre + " no es numérico: " + valor);
		}
		double d = valor.doubleValue();
		if (d < min || d > max) {
			throw new LecturaInvalidaException(nombre + " fuera de rango [" + min + ", " + max + "]: " + d);
		}
		return d;
	}
}
