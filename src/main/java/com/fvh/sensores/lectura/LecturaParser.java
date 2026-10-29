package com.fvh.sensores.lectura;

import java.time.Instant;
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

		JsonNode v = campo(json, "v");
		if (!v.isIntegralNumber() || v.longValue() != 1) {
			throw new LecturaInvalidaException("versión de esquema no soportada: " + v);
		}
		JsonNode ts = campo(json, "ts");
		if (!ts.isIntegralNumber() || !ts.canConvertToLong() || ts.longValue() < TS_MINIMO) {
			throw new LecturaInvalidaException("ts inválido: " + ts);
		}
		double temperatura = numero(json, "temperatura_c", 0, 50);
		double humedad = numero(json, "humedad_pct", 20, 90);

		return new Lectura(m.group(1), Instant.ofEpochSecond(ts.longValue()), temperatura, humedad);
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
