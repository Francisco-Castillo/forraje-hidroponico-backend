package com.fvh.sensores.lectura.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fvh.sensores.lectura.consulta.ConsultaInvalidaException;
import com.fvh.sensores.lectura.consulta.LecturaNoEncontradaException;

/**
 * Errores de la API como {@code ProblemDetail} (RFC 9457). Los errores de binding y validación de Spring ya los
 * resuelve la clase base; aquí solo se agregan los propios, sin exponer detalles internos.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ConsultaInvalidaException.class)
	ProblemDetail consultaInvalida(ConsultaInvalidaException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	@ExceptionHandler(LecturaNoEncontradaException.class)
	ProblemDetail noEncontrada(LecturaNoEncontradaException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}
}
