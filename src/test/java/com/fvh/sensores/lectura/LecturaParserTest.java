package com.fvh.sensores.lectura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.json.JsonMapper;

class LecturaParserTest {

	private static final String TOPIC = "fvh/bandeja-01/lecturas";
	private final LecturaParser parser = new LecturaParser(JsonMapper.builder().build());

	@Test
	void aceptaMensajeValido() {
		Lectura l = parser.parse(TOPIC, "{\"v\":1,\"ts\":1761694800,\"temperatura_c\":24.5,\"humedad_pct\":61.0}");

		assertThat(l).isEqualTo(new Lectura("bandeja-01", Instant.ofEpochSecond(1761694800L), 24.5, 61.0));
	}

	@Test
	void ignoraCamposDesconocidos() {
		Lectura l = parser.parse(TOPIC, "{\"v\":1,\"ts\":1761694800,\"temperatura_c\":24,\"humedad_pct\":61,\"extra\":true}");

		assertThat(l.temperaturaC()).isEqualTo(24.0);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"",
			"no es json",
			"[1,2]",
			"{\"v\":1,\"ts\":1761694800,\"humedad_pct\":61.0}",
			"{\"v\":1,\"ts\":1761694800,\"temperatura_c\":null,\"humedad_pct\":61.0}",
			"{\"v\":1,\"ts\":1761694800,\"temperatura_c\":\"24.5\",\"humedad_pct\":61.0}",
			"{\"v\":2,\"ts\":1761694800,\"temperatura_c\":24.5,\"humedad_pct\":61.0}",
			"{\"ts\":1761694800,\"temperatura_c\":24.5,\"humedad_pct\":61.0}",
			"{\"v\":1,\"ts\":12,\"temperatura_c\":24.5,\"humedad_pct\":61.0}",
			"{\"v\":1,\"ts\":1761694800.5,\"temperatura_c\":24.5,\"humedad_pct\":61.0}",
			"{\"v\":1,\"ts\":1761694800,\"temperatura_c\":-0.1,\"humedad_pct\":61.0}",
			"{\"v\":1,\"ts\":1761694800,\"temperatura_c\":50.1,\"humedad_pct\":61.0}",
			"{\"v\":1,\"ts\":1761694800,\"temperatura_c\":24.5,\"humedad_pct\":19.9}",
			"{\"v\":1,\"ts\":1761694800,\"temperatura_c\":24.5,\"humedad_pct\":90.1}"
	})
	void rechazaPayloadInvalido(String payload) {
		assertThatThrownBy(() -> parser.parse(TOPIC, payload)).isInstanceOf(LecturaInvalidaException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "fvh/lecturas", "fvh/Bandeja/lecturas", "fvh/a/b/lecturas", "fvh//lecturas",
			"otro/bandeja-01/lecturas", "fvh/bandeja-01/otra", "fvh/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa/lecturas" })
	void rechazaTopicInvalido(String topic) {
		String ok = "{\"v\":1,\"ts\":1761694800,\"temperatura_c\":24.5,\"humedad_pct\":61.0}";

		assertThatThrownBy(() -> parser.parse(topic, ok)).isInstanceOf(LecturaInvalidaException.class);
	}

	@Test
	void aceptaLimitesDeRango() {
		assertThat(parser.parse(TOPIC, "{\"v\":1,\"ts\":1704067200,\"temperatura_c\":0,\"humedad_pct\":20}")).isNotNull();
		assertThat(parser.parse(TOPIC, "{\"v\":1,\"ts\":1704067200,\"temperatura_c\":50,\"humedad_pct\":90}")).isNotNull();
	}
}
