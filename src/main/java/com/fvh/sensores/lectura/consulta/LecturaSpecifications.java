package com.fvh.sensores.lectura.consulta;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

/** Criterios componibles para {@link LecturaEntity}. Los valores null no restringen (spec sin restricción, porque and(null) lanza excepción). */
public final class LecturaSpecifications {

	private LecturaSpecifications() {
	}

	public static Specification<LecturaEntity> deNodo(String nodo) {
		return (root, query, cb) -> cb.equal(root.get("nodo"), nodo);
	}

	public static Specification<LecturaEntity> desde(Instant desde) {
		return desde == null ? Specification.unrestricted() : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("id").get("ts"), desde);
	}

	public static Specification<LecturaEntity> antesDe(Instant hasta) {
		return hasta == null ? Specification.unrestricted() : (root, query, cb) -> cb.lessThan(root.get("id").get("ts"), hasta);
	}

	public static Specification<LecturaEntity> temperaturaMin(Double min) {
		return min == null ? Specification.unrestricted() : (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Double>get("temperaturaC"), min);
	}

	public static Specification<LecturaEntity> temperaturaMax(Double max) {
		return max == null ? Specification.unrestricted() : (root, query, cb) -> cb.lessThanOrEqualTo(root.<Double>get("temperaturaC"), max);
	}

	public static Specification<LecturaEntity> humedadMin(Double min) {
		return min == null ? Specification.unrestricted() : (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Double>get("humedadPct"), min);
	}

	public static Specification<LecturaEntity> humedadMax(Double max) {
		return max == null ? Specification.unrestricted() : (root, query, cb) -> cb.lessThanOrEqualTo(root.<Double>get("humedadPct"), max);
	}

	public static Specification<LecturaEntity> deFiltro(LecturaFiltro f) {
		return Specification.where(deNodo(f.deviceId())).and(desde(f.desde())).and(antesDe(f.hasta()))
				.and(temperaturaMin(f.temperaturaMin())).and(temperaturaMax(f.temperaturaMax()))
				.and(humedadMin(f.humedadMin())).and(humedadMax(f.humedadMax()));
	}
}
