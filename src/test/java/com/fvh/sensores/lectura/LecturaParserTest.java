package com.fvh.sensores.lectura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.json.JsonMapper;

class LecturaParserTest {

	private static final String TOPIC = "fvh/esp32-fvh-01/lecturas";
	private static final String VALIDO = mensaje("\"esp32-fvh-01\"", "\"abc-1\"", "\"2026-10-28T20:00:00Z\"", "27.5", "65.0");
	private final LecturaParser parser = new LecturaParser(JsonMapper.builder().build());

	private static String mensaje(String deviceId, String messageId, String timestamp, String temp, String hum) {
		return "{\"deviceId\":" + deviceId + ",\"messageId\":" + messageId + ",\"timestamp\":" + timestamp
				+ ",\"measurements\":{\"temperatureC\":" + temp + ",\"relativeHumidityPct\":" + hum + "}}";
	}

	@Test
	void aceptaMensajeValido() {
		Lectura l = parser.parse(TOPIC, VALIDO);

		assertThat(l).isEqualTo(new Lectura("esp32-fvh-01", "abc-1", Instant.parse("2026-10-28T20:00:00Z"), 27.5, 65.0, null));
	}

	@Test
	void ignoraCamposDesconocidos() {
		String conExtra = VALIDO.substring(0, VALIDO.length() - 1) + ",\"extra\":true}";

		assertThat(parser.parse(TOPIC, conExtra).temperaturaC()).isEqualTo(27.5);
	}

	private static String conSuelo(String valor) {
		return VALIDO.replace("\"relativeHumidityPct\":65.0", "\"relativeHumidityPct\":65.0,\"soilMoistureRaw\":" + valor);
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "2310", "4095" })
	void aceptaHumedadDeSueloEnRango(String valor) {
		assertThat(parser.parse(TOPIC, conSuelo(valor)).humedadSueloRaw()).isEqualTo(Integer.valueOf(valor));
	}

	@Test
	void humedadDeSueloAusenteONulaQuedaNula() {
		assertThat(parser.parse(TOPIC, VALIDO).humedadSueloRaw()).isNull();
		assertThat(parser.parse(TOPIC, conSuelo("null")).humedadSueloRaw()).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "\"2310\"", "2310.5", "-1", "4096", "true", "99999999999" })
	void rechazaHumedadDeSueloInvalida(String valor) {
		assertThatThrownBy(() -> parser.parse(TOPIC, conSuelo(valor))).isInstanceOf(LecturaInvalidaException.class);
	}

	@Test
	void aceptaFraccionDeSegundoYLimitesDeRango() {
		assertThat(parser.parse(TOPIC, mensaje("\"esp32-fvh-01\"", "\"a\"", "\"2026-10-28T20:00:00.123Z\"", "0", "20"))).isNotNull();
		assertThat(parser.parse(TOPIC, mensaje("\"esp32-fvh-01\"", "\"a\"", "\"2026-10-28T20:00:00Z\"", "50", "90"))).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"",
			"no es json",
			"[1,2]",
			// campos ausentes o nulos
			"{\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\"}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":null,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":5}",
			// tipos erróneos
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":\"27.5\",\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":7,\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":1761681600,\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			// messageId vacío o demasiado largo
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"  \",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			// timestamp: sin Z, con desfase, basura, reloj sin sincronizar
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00+00:00\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"ayer\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"1970-01-01T00:00:12Z\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			// deviceId distinto del tópico
			"{\"deviceId\":\"otro-nodo\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":65}}",
			// fuera de rango
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":-0.1,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":50.1,\"relativeHumidityPct\":65}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":19.9}}",
			"{\"deviceId\":\"esp32-fvh-01\",\"messageId\":\"a\",\"timestamp\":\"2026-10-28T20:00:00Z\",\"measurements\":{\"temperatureC\":27.5,\"relativeHumidityPct\":90.1}}"
	})
	void rechazaPayloadInvalido(String payload) {
		assertThatThrownBy(() -> parser.parse(TOPIC, payload)).isInstanceOf(LecturaInvalidaException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "fvh/lecturas", "fvh/Bandeja/lecturas", "fvh/a/b/lecturas", "fvh//lecturas",
			"otro/esp32-fvh-01/lecturas", "fvh/esp32-fvh-01/otra", "fvh/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/lecturas" })
	void rechazaTopicInvalido(String topic) {
		assertThatThrownBy(() -> parser.parse(topic, VALIDO)).isInstanceOf(LecturaInvalidaException.class);
	}
}
