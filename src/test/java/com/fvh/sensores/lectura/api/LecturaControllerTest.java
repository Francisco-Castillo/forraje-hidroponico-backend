package com.fvh.sensores.lectura.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fvh.sensores.lectura.consulta.ConsultaInvalidaException;
import com.fvh.sensores.lectura.consulta.LecturaConsultaService;
import com.fvh.sensores.lectura.consulta.LecturaEntity;
import com.fvh.sensores.lectura.consulta.LecturaFiltro;
import com.fvh.sensores.lectura.consulta.LecturaId;
import com.fvh.sensores.lectura.consulta.LecturaNoEncontradaException;

@WebMvcTest(LecturaControllerImpl.class)
@Import({ LecturaMapperImpl.class, ApiExceptionHandler.class })
class LecturaControllerTest {

	private static final String BASE = "/api/v1/dispositivos/esp32-fvh-01/lecturas";
	private static final String RANGO = "?desde=2026-10-28T00:00:00Z&hasta=2026-10-29T00:00:00Z";

	@Autowired
	MockMvc mvc;

	@MockitoBean
	LecturaConsultaService service;

	private static LecturaEntity entidad() {
		LecturaEntity e = mock(LecturaEntity.class);
		when(e.getId()).thenReturn(new LecturaId("m-1", Instant.parse("2026-10-28T20:00:00Z")));
		when(e.getNodo()).thenReturn("esp32-fvh-01");
		when(e.getTemperaturaC()).thenReturn(27.5);
		when(e.getHumedadPct()).thenReturn(65.0);
		when(e.getHumedadSueloRaw()).thenReturn(2310);
		return e;
	}

	@Test
	void ultimaDevuelveElContrato() throws Exception {
		LecturaEntity e = entidad();
		when(service.ultima("esp32-fvh-01")).thenReturn(e);

		mvc.perform(get(BASE + "/ultima")).andExpect(status().isOk())
				.andExpect(jsonPath("$.deviceId").value("esp32-fvh-01"))
				.andExpect(jsonPath("$.messageId").value("m-1"))
				.andExpect(jsonPath("$.timestamp").value("2026-10-28T20:00:00Z"))
				.andExpect(jsonPath("$.temperatureC").value(27.5))
				.andExpect(jsonPath("$.relativeHumidityPct").value(65.0))
				.andExpect(jsonPath("$.soilMoistureRaw").value(2310));
	}

	@Test
	void ultimaSinMedicionesEs404ProblemDetail() throws Exception {
		when(service.ultima("esp32-fvh-01")).thenThrow(new LecturaNoEncontradaException("sin mediciones"));

		mvc.perform(get(BASE + "/ultima")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.detail").value("sin mediciones"));
	}

	@Test
	void deviceIdInvalidoEs400SinLlamarAlServicio() throws Exception {
		mvc.perform(get("/api/v1/dispositivos/NO_VALIDO/lecturas/ultima")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/dispositivos/NO_VALIDO/lecturas" + RANGO)).andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void historialDevuelvePaginaYPasaLosParametros() throws Exception {
		LecturaEntity e = entidad();
		when(service.historial(any(), eq(1), eq(10), eq("temperatureC,desc")))
				.thenReturn(new PageImpl<>(List.of(e), PageRequest.of(1, 10), 11));

		mvc.perform(get(BASE + RANGO + "&temperaturaMin=20&humedadMax=70.5&page=1&size=10&sort=temperatureC,desc"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].messageId").value("m-1"))
				.andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalElements").value(11)).andExpect(jsonPath("$.totalPages").value(2));

		ArgumentCaptor<LecturaFiltro> c = ArgumentCaptor.forClass(LecturaFiltro.class);
		verify(service).historial(c.capture(), eq(1), eq(10), eq("temperatureC,desc"));
		LecturaFiltro f = c.getValue();
		org.assertj.core.api.Assertions.assertThat(f.deviceId()).isEqualTo("esp32-fvh-01");
		org.assertj.core.api.Assertions.assertThat(f.desde()).isEqualTo(Instant.parse("2026-10-28T00:00:00Z"));
		org.assertj.core.api.Assertions.assertThat(f.hasta()).isEqualTo(Instant.parse("2026-10-29T00:00:00Z"));
		org.assertj.core.api.Assertions.assertThat(f.temperaturaMin()).isEqualTo(20.0);
		org.assertj.core.api.Assertions.assertThat(f.humedadMax()).isEqualTo(70.5);
		org.assertj.core.api.Assertions.assertThat(f.temperaturaMax()).isNull();
	}

	@Test
	void parametrosPorDefecto() throws Exception {
		when(service.historial(any(), eq(0), eq(20), eq(null))).thenReturn(new PageImpl<>(List.of()));

		mvc.perform(get(BASE + RANGO)).andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	void faltanDesdeOHastaEs400() throws Exception {
		mvc.perform(get(BASE + "?hasta=2026-10-29T00:00:00Z")).andExpect(status().isBadRequest());
		mvc.perform(get(BASE + "?desde=2026-10-28T00:00:00Z")).andExpect(status().isBadRequest());
		mvc.perform(get(BASE)).andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void tiposInvalidosSon400() throws Exception {
		mvc.perform(get(BASE + "?desde=ayer&hasta=2026-10-29T00:00:00Z")).andExpect(status().isBadRequest());
		mvc.perform(get(BASE + RANGO + "&temperaturaMin=abc")).andExpect(status().isBadRequest());
		mvc.perform(get(BASE + RANGO + "&page=x")).andExpect(status().isBadRequest());
		mvc.perform(get(BASE + RANGO + "&page=-1")).andExpect(status().isBadRequest());
		mvc.perform(get(BASE + RANGO + "&size=0")).andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void reglasDeNegocioDelServicioSon400ProblemDetail() throws Exception {
		when(service.historial(any(), eq(0), eq(20), eq(null))).thenThrow(new ConsultaInvalidaException("el intervalo supera el máximo"));

		mvc.perform(get(BASE + RANGO)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").value("el intervalo supera el máximo"));
	}
}
