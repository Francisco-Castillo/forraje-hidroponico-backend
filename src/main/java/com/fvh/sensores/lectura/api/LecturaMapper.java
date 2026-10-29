package com.fvh.sensores.lectura.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

import com.fvh.sensores.lectura.api.dto.LecturaResponse;
import com.fvh.sensores.lectura.api.dto.PaginaResponse;
import com.fvh.sensores.lectura.consulta.LecturaEntity;

@Mapper(componentModel = "spring")
public interface LecturaMapper {

	@Mapping(target = "deviceId", source = "nodo")
	@Mapping(target = "messageId", source = "id.messageId")
	@Mapping(target = "timestamp", source = "id.ts")
	@Mapping(target = "temperatureC", source = "temperaturaC")
	@Mapping(target = "relativeHumidityPct", source = "humedadPct")
	LecturaResponse toResponse(LecturaEntity entity);

	default PaginaResponse<LecturaResponse> toPagina(Page<LecturaEntity> page) {
		return new PaginaResponse<>(page.getContent().stream().map(this::toResponse).toList(), page.getNumber(),
				page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
