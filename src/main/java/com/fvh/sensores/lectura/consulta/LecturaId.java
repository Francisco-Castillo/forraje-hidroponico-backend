package com.fvh.sensores.lectura.consulta;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Clave primaria de la hypertable: (message_id, ts). */
@Embeddable
public record LecturaId(@Column(name = "message_id") String messageId, @Column(name = "ts") Instant ts) {
}
