package com.fvh.sensores.lectura.consulta;

/** Parámetros de consulta inválidos; se traduce en HTTP 400. */
public class ConsultaInvalidaException extends RuntimeException {

	public ConsultaInvalidaException(String message) {
		super(message);
	}
}
