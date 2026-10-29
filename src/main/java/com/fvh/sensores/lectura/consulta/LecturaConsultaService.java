package com.fvh.sensores.lectura.consulta;

import org.springframework.data.domain.Page;

public interface LecturaConsultaService {

	/** @throws ConsultaInvalidaException si deviceId es inválido. @throws LecturaNoEncontradaException si no hay mediciones. */
	LecturaEntity ultima(String deviceId);

	/**
	 * Historial de un dispositivo.
	 *
	 * @param sort {@code campo[,asc|desc]} con campo en {ts, temperatureC, relativeHumidityPct}; null = {@code ts,desc}
	 * @throws ConsultaInvalidaException si algún parámetro incumple las reglas (rango, tamaño, orden, límites)
	 */
	Page<LecturaEntity> historial(LecturaFiltro filtro, int page, int size, String sort);
}
