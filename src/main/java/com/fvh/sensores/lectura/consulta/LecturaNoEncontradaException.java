package com.fvh.sensores.lectura.consulta;

/** El dispositivo no tiene mediciones; se traduce en HTTP 404. */
public class LecturaNoEncontradaException extends RuntimeException {

	public LecturaNoEncontradaException(String message) {
		super(message);
	}
}
