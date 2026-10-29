package com.fvh.sensores.lectura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Camino completo con TimescaleDB y Mosquitto reales: MQTT -> validación -> tabla lecturas. */
@SpringBootTest
@Testcontainers
class LecturasIntegrationTest {

	@Container
	static final PostgreSQLContainer DB = new PostgreSQLContainer(
			DockerImageName.parse("timescale/timescaledb:2.31.0-pg17").asCompatibleSubstituteFor("postgres"));

	@Container
	static final GenericContainer<?> BROKER = new GenericContainer<>("eclipse-mosquitto:2.1.2-alpine")
			.withExposedPorts(1883)
			.withCopyToContainer(Transferable.of("listener 1883\nallow_anonymous true\n"),
					"/mosquitto/config/mosquitto.conf")
			.waitingFor(Wait.forListeningPort());

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry r) {
		r.add("spring.datasource.url", DB::getJdbcUrl);
		r.add("spring.datasource.username", DB::getUsername);
		r.add("spring.datasource.password", DB::getPassword);
		r.add("sensores.mqtt.url", () -> "tcp://" + BROKER.getHost() + ":" + BROKER.getMappedPort(1883));
		r.add("sensores.mqtt.username", () -> "");
		r.add("sensores.mqtt.password", () -> "");
	}

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void limpiar() {
		jdbc.update("TRUNCATE lecturas");
	}

	@Test
	void lecturasEsHypertable() {
		Integer n = jdbc.queryForObject(
				"SELECT count(*) FROM timescaledb_information.hypertables WHERE hypertable_name = 'lecturas'",
				Integer.class);

		assertThat(n).isEqualTo(1);
	}

	private static String json(String deviceId, String messageId, String timestamp, String temp) {
		return "{\"deviceId\":\"" + deviceId + "\",\"messageId\":\"" + messageId + "\",\"timestamp\":\"" + timestamp
				+ "\",\"measurements\":{\"temperatureC\":" + temp + ",\"relativeHumidityPct\":61.0}}";
	}

	@Test
	void persisteLecturaValida() throws Exception {
		publicar("fvh/bandeja-01/lecturas", json("bandeja-01", "m-1", "2026-10-28T20:00:00Z", "24.5"));

		await().atMost(Duration.ofSeconds(15)).until(() -> contar() == 1);
		var fila = jdbc.queryForMap(
				"SELECT message_id, nodo, temperatura_c, humedad_pct, extract(epoch FROM ts)::bigint AS ts FROM lecturas");
		assertThat(fila).containsEntry("message_id", "m-1").containsEntry("nodo", "bandeja-01")
				.containsEntry("temperatura_c", 24.5).containsEntry("humedad_pct", 61.0)
				.containsEntry("ts", 1793217600L);
	}

	@Test
	void persisteHumedadDeSueloOpcionalYNulaSiNoViene() throws Exception {
		String ts = "2026-10-28T20:00:00Z";
		publicar("fvh/bandeja-04/lecturas", json("bandeja-04", "s-1", ts, "24.5").replace("61.0}", "61.0,\"soilMoistureRaw\":3228}"));
		publicar("fvh/bandeja-04/lecturas", json("bandeja-04", "s-2", ts, "24.5"));
		publicar("fvh/bandeja-04/lecturas", json("bandeja-04", "s-3", ts, "24.5").replace("61.0}", "61.0,\"soilMoistureRaw\":5000}"));

		await().atMost(Duration.ofSeconds(15)).until(() -> contar() == 2);
		Thread.sleep(1000);
		assertThat(contar()).isEqualTo(2);
		assertThat(jdbc.queryForObject("SELECT humedad_suelo_raw FROM lecturas WHERE message_id = 's-1'", Integer.class))
				.isEqualTo(3228);
		assertThat(jdbc.queryForObject("SELECT humedad_suelo_raw FROM lecturas WHERE message_id = 's-2'", Integer.class))
				.isNull();
	}

	@Test
	void ignoraDuplicadosYDescartaInvalidos() throws Exception {
		String ts = "2026-10-28T20:00:00Z";
		publicar("fvh/bandeja-02/lecturas", "no es json");
		publicar("fvh/bandeja-02/lecturas", json("bandeja-02", "m-1", ts, "80"));
		publicar("fvh/bandeja-02/lecturas", json("otro-nodo", "m-2", ts, "24.5"));
		publicar("fvh/bandeja-02/lecturas", json("bandeja-02", "m-3", ts, "24.5"));
		publicar("fvh/bandeja-02/lecturas", json("bandeja-02", "m-3", ts, "24.5"));
		// mismo messageId y mismo ts en otro nodo: también es duplicado (el id es único global)
		publicar("fvh/bandeja-03/lecturas", json("bandeja-03", "m-3", ts, "24.5"));
		// otro messageId en otro nodo: se guarda
		publicar("fvh/bandeja-03/lecturas", json("bandeja-03", "m-4", ts, "24.5"));

		await().atMost(Duration.ofSeconds(15)).until(() -> contar() == 2);
		// los mensajes llegan en orden; margen para que se procesen los duplicados
		Thread.sleep(1000);
		assertThat(contar()).isEqualTo(2);
	}

	private int contar() {
		return jdbc.queryForObject("SELECT count(*) FROM lecturas", Integer.class);
	}

	private static void publicar(String topic, String payload) throws Exception {
		MqttClient client = new MqttClient("tcp://" + BROKER.getHost() + ":" + BROKER.getMappedPort(1883),
				"test-" + UUID.randomUUID(), new MemoryPersistence());
		try {
			client.connect();
			client.publish(topic, new MqttMessage(payload.getBytes()) {{ setQos(1); }});
		} finally {
			client.disconnect();
			client.close();
		}
	}
}
