package com.fvh.sensores.lectura;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LecturaRepository {

	private final JdbcTemplate jdbc;

	public LecturaRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** @return true si se insertó; false si ya existía (nodo, ts), p. ej. un duplicado de QoS 1. */
	public boolean guardar(Lectura l) {
		return jdbc.update("""
				INSERT INTO lecturas (ts, nodo, temperatura_c, humedad_pct)
				VALUES (?, ?, ?, ?)
				ON CONFLICT (nodo, ts) DO NOTHING
				""", OffsetDateTime.ofInstant(l.ts(), ZoneOffset.UTC), l.nodo(), l.temperaturaC(), l.humedadPct()) == 1;
	}
}
