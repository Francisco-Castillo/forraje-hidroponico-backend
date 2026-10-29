package com.fvh.sensores.lectura.consulta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class LecturaConsultaServiceImplTest {

	private static final Instant DESDE = Instant.parse("2026-10-28T00:00:00Z");
	private static final Instant HASTA = Instant.parse("2026-10-29T00:00:00Z");

	LecturaJpaRepository repository = mock(LecturaJpaRepository.class);
	LecturaConsultaServiceImpl service;

	@BeforeEach
	void init() {
		service = new LecturaConsultaServiceImpl(repository, new ConsultaProperties(Duration.ofDays(7), 100));
	}

	private static LecturaFiltro filtro(String id, Instant desde, Instant hasta) {
		return new LecturaFiltro(id, desde, hasta, null, null, null, null);
	}

	@SuppressWarnings("unchecked")
	private Pageable consultar(LecturaFiltro f, int page, int size, String sort) {
		when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(java.util.List.of()));
		service.historial(f, page, size, sort);
		ArgumentCaptor<Pageable> c = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).findAll(any(Specification.class), c.capture());
		return c.getValue();
	}

	@Test
	void ultimaDevuelveLaMedicion() {
		LecturaEntity e = mock(LecturaEntity.class);
		when(repository.findFirstByNodoOrderByIdTsDesc("esp32-fvh-01")).thenReturn(Optional.of(e));

		assertThat(service.ultima("esp32-fvh-01")).isSameAs(e);
	}

	@Test
	void ultimaSinMedicionesLanzaNoEncontrada() {
		when(repository.findFirstByNodoOrderByIdTsDesc("nuevo")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.ultima("nuevo")).isInstanceOf(LecturaNoEncontradaException.class);
	}

	@Test
	void deviceIdInvalidoNoConsultaLaBase() {
		for (String id : new String[] { "ESP32", "a b", "x'; DROP TABLE lecturas;--", "", "a".repeat(33), null }) {
			assertThatThrownBy(() -> service.ultima(id)).isInstanceOf(ConsultaInvalidaException.class);
			assertThatThrownBy(() -> service.historial(filtro(id, DESDE, HASTA), 0, 20, null))
					.isInstanceOf(ConsultaInvalidaException.class);
		}
		verify(repository, never()).findFirstByNodoOrderByIdTsDesc(any());
	}

	@Test
	void rangoInvalido() {
		assertThatThrownBy(() -> service.historial(filtro("n1", null, HASTA), 0, 20, null)).isInstanceOf(ConsultaInvalidaException.class);
		assertThatThrownBy(() -> service.historial(filtro("n1", DESDE, null), 0, 20, null)).isInstanceOf(ConsultaInvalidaException.class);
		assertThatThrownBy(() -> service.historial(filtro("n1", HASTA, DESDE), 0, 20, null)).hasMessageContaining("anterior");
		assertThatThrownBy(() -> service.historial(filtro("n1", DESDE, DESDE), 0, 20, null)).hasMessageContaining("anterior");
		assertThatThrownBy(() -> service.historial(filtro("n1", DESDE, DESDE.plus(Duration.ofDays(7)).plusSeconds(1)), 0, 20, null))
				.hasMessageContaining("máximo");
	}

	@Test
	void rangoDeExactamenteElMaximoSeAcepta() {
		consultar(filtro("n1", DESDE, DESDE.plus(Duration.ofDays(7))), 0, 20, null);
	}

	@Test
	void paginacionFueraDeLimites() {
		assertThatThrownBy(() -> service.historial(filtro("n1", DESDE, HASTA), -1, 20, null)).isInstanceOf(ConsultaInvalidaException.class);
		assertThatThrownBy(() -> service.historial(filtro("n1", DESDE, HASTA), 0, 0, null)).isInstanceOf(ConsultaInvalidaException.class);
		assertThatThrownBy(() -> service.historial(filtro("n1", DESDE, HASTA), 0, 101, null)).isInstanceOf(ConsultaInvalidaException.class);
		verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	void limitesDeValoresInvalidos() {
		assertThatThrownBy(() -> service.historial(new LecturaFiltro("n1", DESDE, HASTA, 30.0, 20.0, null, null), 0, 20, null))
				.hasMessageContaining("temperatura");
		assertThatThrownBy(() -> service.historial(new LecturaFiltro("n1", DESDE, HASTA, null, null, 80.0, 40.0), 0, 20, null))
				.hasMessageContaining("humedad");
		assertThatThrownBy(() -> service.historial(new LecturaFiltro("n1", DESDE, HASTA, Double.NaN, null, null, null), 0, 20, null))
				.hasMessageContaining("finitos");
	}

	@Test
	void ordenPorDefectoEsTsDescendente() {
		Pageable p = consultar(filtro("n1", DESDE, HASTA), 2, 50, null);

		assertThat(p.getPageNumber()).isEqualTo(2);
		assertThat(p.getPageSize()).isEqualTo(50);
		assertThat(p.getSort().getOrderFor("id.ts").getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void ordenTraduceNombrePublicoAPropiedad() {
		Pageable p = consultar(filtro("n1", DESDE, HASTA), 0, 20, "temperatureC,desc");

		assertThat(p.getSort().getOrderFor("temperaturaC").getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(p.getSort().getOrderFor("id.ts")).isNotNull();
	}

	@Test
	void ordenSinDireccionEsAscendente() {
		Pageable p = consultar(filtro("n1", DESDE, HASTA), 0, 20, "relativeHumidityPct");

		assertThat(p.getSort().getOrderFor("humedadPct").getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void ordenFueraDeListaBlancaSeRechaza() {
		for (String sort : new String[] { "nodo", "password,asc", "ts,sideways", "ts,asc,desc", "id.ts", "temperaturaC" }) {
			assertThatThrownBy(() -> service.historial(filtro("n1", DESDE, HASTA), 0, 20, sort))
					.as(sort).isInstanceOf(ConsultaInvalidaException.class);
		}
	}

	@Test
	void devuelveLaPaginaDelRepositorio() {
		LecturaEntity e = mock(LecturaEntity.class);
		Page<LecturaEntity> pagina = new PageImpl<>(java.util.List.of(e));
		when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagina);

		assertThat(service.historial(filtro("n1", DESDE, HASTA), 0, 20, null)).isSameAs(pagina);
	}
}
