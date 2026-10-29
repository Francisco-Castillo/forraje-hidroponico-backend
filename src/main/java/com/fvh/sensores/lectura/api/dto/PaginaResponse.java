package com.fvh.sensores.lectura.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Página de resultados")
public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
