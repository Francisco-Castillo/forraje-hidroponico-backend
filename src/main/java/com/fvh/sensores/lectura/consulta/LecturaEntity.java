package com.fvh.sensores.lectura.consulta;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Vista de solo lectura de la tabla {@code lecturas}. La escritura sigue en {@code LecturaRepository}
 * (JdbcTemplate), por eso la entidad es inmutable.
 */
@Entity
@Immutable
@Table(name = "lecturas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LecturaEntity {

	@EmbeddedId
	private LecturaId id;

	@Column(name = "nodo", nullable = false)
	private String nodo;

	@Column(name = "temperatura_c", nullable = false)
	private double temperaturaC;

	@Column(name = "humedad_pct", nullable = false)
	private double humedadPct;

	@Column(name = "humedad_suelo_raw")
	private Integer humedadSueloRaw;
}
