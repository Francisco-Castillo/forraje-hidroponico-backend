package com.fvh.sensores.lectura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LecturaService {

	private static final Logger log = LoggerFactory.getLogger(LecturaService.class);

	private final LecturaParser parser;
	private final LecturaRepository repository;

	public LecturaService(LecturaParser parser, LecturaRepository repository) {
		this.parser = parser;
		this.repository = repository;
	}

	public void recibir(String topic, String payload) {
		Lectura lectura;
		try {
			lectura = parser.parse(topic, payload);
		} catch (LecturaInvalidaException e) {
			log.warn("Mensaje descartado ({}): {}", e.getMessage(), payload);
			return;
		}
		if (!repository.guardar(lectura)) {
			log.debug("Lectura duplicada ignorada: {} {}", lectura.nodo(), lectura.ts());
		}
	}
}
