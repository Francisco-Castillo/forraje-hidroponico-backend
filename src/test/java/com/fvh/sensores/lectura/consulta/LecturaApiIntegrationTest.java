package com.fvh.sensores.lectura.consulta;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.fvh.sensores.lectura.Lectura;
import com.fvh.sensores.lectura.LecturaRepository;

/** API HTTP contra TimescaleDB real: filtros, orden, paginación y última medición sobre filas insertadas. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LecturaApiIntegrationTest {

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

	private static final String BASE = "/api/v1/dispositivos/api-01/lecturas";
	private static final Instant T0 = Instant.parse("2026-10-28T20:00:00Z");

	@Autowired
	MockMvc mvc;
	@Autowired
	JdbcTemplate jdbc;
	@Autowired
	LecturaRepository escritor;

	@BeforeEach
	void datos() {
		jdbc.update("TRUNCATE lecturas");
		// api-01: 5 lecturas, una por minuto, temperatura 20..24 y humedad 50..54
		for (int i = 0; i < 5; i++) {
			escritor.guardar(new Lectura("api-01", "a-" + i, T0.plusSeconds(60L * i), 20.0 + i, 50.0 + i, 3000 + i));
		}
		escritor.guardar(new Lectura("api-02", "b-0", T0, 30.0, 70.0, null));
	}

	private static String rango(String desde, String hasta) {
		return "?desde=" + desde + "&hasta=" + hasta;
	}

	private static final String TODO = rango("2026-10-28T20:00:00Z", "2026-10-28T21:00:00Z");

	@Test
	void indiceDeNodoYTiempoExiste() {
		Integer n = jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE indexname = 'lecturas_nodo_ts_idx'", Integer.class);
		org.assertj.core.api.Assertions.assertThat(n).isEqualTo(1);
	}

	@Test
	void ultimaDevuelveLaMasRecienteDelDispositivo() throws Exception {
		mvc.perform(get(BASE + "/ultima")).andExpect(status().isOk())
				.andExpect(jsonPath("$.deviceId").value("api-01")).andExpect(jsonPath("$.messageId").value("a-4"))
				.andExpect(jsonPath("$.timestamp").value("2026-10-28T20:04:00Z"))
				.andExpect(jsonPath("$.temperatureC").value(24.0)).andExpect(jsonPath("$.relativeHumidityPct").value(54.0));
	}

	@Test
	void ultimaIncluyeHumedadDeSueloYNulaSiNoLaTraia() throws Exception {
		mvc.perform(get(BASE + "/ultima")).andExpect(jsonPath("$.soilMoistureRaw").value(3004));
		mvc.perform(get("/api/v1/dispositivos/api-02/lecturas/ultima")).andExpect(status().isOk())
				.andExpect(jsonPath("$.soilMoistureRaw").doesNotExist());
	}

	@Test
	void ultimaDeDispositivoSinDatosEs404() throws Exception {
		mvc.perform(get("/api/v1/dispositivos/no-existe/lecturas/ultima")).andExpect(status().isNotFound());
	}

	@Test
	void historialPorDefectoEsDescendenteYSoloDelDispositivo() throws Exception {
		mvc.perform(get(BASE + TODO)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(5))
				.andExpect(jsonPath("$.content[0].messageId").value("a-4"))
				.andExpect(jsonPath("$.content[4].messageId").value("a-0"));
	}

	@Test
	void intervaloEsInclusivoEnDesdeYExclusivoEnHasta() throws Exception {
		mvc.perform(get(BASE + rango("2026-10-28T20:01:00Z", "2026-10-28T20:03:00Z") + "&sort=ts,asc"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].messageId").value("a-1"))
				.andExpect(jsonPath("$.content[1].messageId").value("a-2"));
	}

	@Test
	void filtrosPorValores() throws Exception {
		mvc.perform(get(BASE + TODO + "&temperaturaMin=21&temperaturaMax=23&sort=ts,asc")).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.content[0].messageId").value("a-1"));
		mvc.perform(get(BASE + TODO + "&humedadMin=53")).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));
		mvc.perform(get(BASE + TODO + "&humedadMax=50")).andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].messageId").value("a-0"));
	}

	@Test
	void ordenPorTemperaturaDescendente() throws Exception {
		mvc.perform(get(BASE + TODO + "&sort=temperatureC,desc")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].temperatureC").value(24.0))
				.andExpect(jsonPath("$.content[4].temperatureC").value(20.0));
	}

	@Test
	void paginacion() throws Exception {
		mvc.perform(get(BASE + TODO + "&sort=ts,asc&size=2&page=2")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1)).andExpect(jsonPath("$.content[0].messageId").value("a-4"))
				.andExpect(jsonPath("$.totalElements").value(5)).andExpect(jsonPath("$.totalPages").value(3))
				.andExpect(jsonPath("$.page").value(2)).andExpect(jsonPath("$.size").value(2));
	}

	@Test
	void validacionesDeLaPeticion() throws Exception {
		mvc.perform(get(BASE + rango("2026-10-28T20:00:00Z", "2026-11-28T20:00:00Z"))).andExpect(status().isBadRequest());
		mvc.perform(get(BASE + rango("2026-10-28T21:00:00Z", "2026-10-28T20:00:00Z"))).andExpect(status().isBadRequest());
		mvc.perform(get(BASE + TODO + "&size=101")).andExpect(status().isBadRequest());
		mvc.perform(get(BASE + TODO + "&sort=nodo")).andExpect(status().isBadRequest());
		mvc.perform(get(BASE + TODO + "&sort=ts%3Bdrop")).andExpect(status().isBadRequest());
	}
}
