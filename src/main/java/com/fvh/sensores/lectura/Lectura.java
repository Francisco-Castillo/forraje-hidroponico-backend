package com.fvh.sensores.lectura;

import java.time.Instant;

public record Lectura(String nodo, String messageId, Instant ts, double temperaturaC, double humedadPct,
		Integer humedadSueloRaw) {
}
