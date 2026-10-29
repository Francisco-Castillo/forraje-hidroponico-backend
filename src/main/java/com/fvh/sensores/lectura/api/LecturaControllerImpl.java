package com.fvh.sensores.lectura.api;

import java.time.Instant;

import org.springframework.web.bind.annotation.RestController;

import com.fvh.sensores.lectura.api.dto.LecturaResponse;
import com.fvh.sensores.lectura.api.dto.PaginaResponse;
import com.fvh.sensores.lectura.consulta.LecturaConsultaService;
import com.fvh.sensores.lectura.consulta.LecturaFiltro;

@RestController
class LecturaControllerImpl implements LecturaController {

	private final LecturaConsultaService service;
	private final LecturaMapper mapper;

	LecturaControllerImpl(LecturaConsultaService service, LecturaMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@Override
	public LecturaResponse ultima(String deviceId) {
		return mapper.toResponse(service.ultima(deviceId));
	}

	@Override
	public PaginaResponse<LecturaResponse> historial(String deviceId, Instant desde, Instant hasta, Double temperaturaMin,
			Double temperaturaMax, Double humedadMin, Double humedadMax, int page, int size, String sort) {
		LecturaFiltro filtro = new LecturaFiltro(deviceId, desde, hasta, temperaturaMin, temperaturaMax, humedadMin,
				humedadMax);
		return mapper.toPagina(service.historial(filtro, page, size, sort));
	}
}
