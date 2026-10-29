package com.fvh.sensores.lectura.consulta;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Límites de la API de consulta ({@code sensores.api.*}). */
@ConfigurationProperties("sensores.api")
public record ConsultaProperties(Duration maxRango, int maxPageSize) {

	public ConsultaProperties {
		if (maxRango == null) {
			maxRango = Duration.ofDays(7);
		}
		if (maxPageSize <= 0) {
			maxPageSize = 100;
		}
	}
}
