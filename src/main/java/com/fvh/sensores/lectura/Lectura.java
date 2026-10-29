package com.fvh.sensores.lectura;

import java.time.Instant;

public record Lectura(String nodo, Instant ts, double temperaturaC, double humedadPct) {
}
