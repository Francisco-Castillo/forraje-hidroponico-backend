package com.fvh.sensores.lectura.consulta;

import java.time.Instant;

/** Criterios del historial. {@code desde} es inclusivo y {@code hasta} exclusivo. Los límites opcionales pueden ser null. */
public record LecturaFiltro(String deviceId, Instant desde, Instant hasta, Double temperaturaMin, Double temperaturaMax,
		Double humedadMin, Double humedadMax) {
}
