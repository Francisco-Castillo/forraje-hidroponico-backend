package com.fvh.sensores.lectura.consulta;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class LecturaConsultaServiceImpl implements LecturaConsultaService {

	private static final Pattern DEVICE_ID = Pattern.compile("^[a-z0-9-]{1,32}$");
	/** Nombre público (contrato) -> propiedad de la entidad. Lista blanca: nada más es ordenable. */
	private static final Map<String, String> CAMPOS_ORDEN = Map.of("ts", "id.ts", "temperatureC", "temperaturaC",
			"relativeHumidityPct", "humedadPct");

	private final LecturaJpaRepository repository;
	private final ConsultaProperties props;

	LecturaConsultaServiceImpl(LecturaJpaRepository repository, ConsultaProperties props) {
		this.repository = repository;
		this.props = props;
	}

	@Override
	public LecturaEntity ultima(String deviceId) {
		validarDeviceId(deviceId);
		return repository.findFirstByNodoOrderByIdTsDesc(deviceId)
				.orElseThrow(() -> new LecturaNoEncontradaException("el dispositivo '" + deviceId + "' no tiene mediciones"));
	}

	@Override
	public Page<LecturaEntity> historial(LecturaFiltro f, int page, int size, String sort) {
		validarDeviceId(f.deviceId());
		validarRango(f);
		validarLimites("temperatura", f.temperaturaMin(), f.temperaturaMax());
		validarLimites("humedad", f.humedadMin(), f.humedadMax());
		if (page < 0) {
			throw new ConsultaInvalidaException("page no puede ser negativo");
		}
		if (size < 1 || size > props.maxPageSize()) {
			throw new ConsultaInvalidaException("size debe estar entre 1 y " + props.maxPageSize());
		}
		return repository.findAll(LecturaSpecifications.deFiltro(f), PageRequest.of(page, size, orden(sort)));
	}

	private static void validarDeviceId(String deviceId) {
		if (deviceId == null || !DEVICE_ID.matcher(deviceId).matches()) {
			throw new ConsultaInvalidaException("deviceId inválido: debe cumplir [a-z0-9-]{1,32}");
		}
	}

	private void validarRango(LecturaFiltro f) {
		if (f.desde() == null || f.hasta() == null) {
			throw new ConsultaInvalidaException("desde y hasta son obligatorios");
		}
		if (!f.desde().isBefore(f.hasta())) {
			throw new ConsultaInvalidaException("desde debe ser anterior a hasta");
		}
		if (Duration.between(f.desde(), f.hasta()).compareTo(props.maxRango()) > 0) {
			throw new ConsultaInvalidaException("el intervalo supera el máximo de " + props.maxRango().toDays() + " días");
		}
	}

	private static void validarLimites(String nombre, Double min, Double max) {
		if ((min != null && !Double.isFinite(min)) || (max != null && !Double.isFinite(max))) {
			throw new ConsultaInvalidaException("los límites de " + nombre + " deben ser números finitos");
		}
		if (min != null && max != null && min > max) {
			throw new ConsultaInvalidaException(nombre + ": el mínimo no puede superar al máximo");
		}
	}

	private static Sort orden(String sort) {
		if (sort == null || sort.isBlank()) {
			return Sort.by(Sort.Direction.DESC, "id.ts");
		}
		String[] partes = sort.split(",", -1);
		String propiedad = CAMPOS_ORDEN.get(partes[0].trim());
		if (propiedad == null || partes.length > 2) {
			throw new ConsultaInvalidaException("sort inválido; campos permitidos: " + CAMPOS_ORDEN.keySet());
		}
		Sort.Direction dir = Sort.Direction.ASC;
		if (partes.length == 2) {
			dir = switch (partes[1].trim().toLowerCase()) {
				case "asc" -> Sort.Direction.ASC;
				case "desc" -> Sort.Direction.DESC;
				default -> throw new ConsultaInvalidaException("la dirección de sort debe ser asc o desc");
			};
		}
		// id.ts como desempate para que la paginación sea estable
		return Sort.by(dir, propiedad).and(Sort.by(Sort.Direction.DESC, "id.ts"));
	}
}
