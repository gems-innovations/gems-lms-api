package com.gems.education.application;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cuántas acciones hace cada usuario por minuto, en memoria (por instancia). Atómico por usuario, así que peticiones en
 * paralelo no lo saltan. La memoria está acotada: si se llena con ventanas vivas, los nuevos pasan sin vigilar.
 */
final class LimitePorMinuto {
  static final long VENTANA_MS = 60_000;
  static final int MAX_VIGILADOS = 50_000;

  /** Ventana de un minuto: cuándo empezó y cuántas acciones lleva (inmutable para leerla sin bloqueo). */
  private record Ventana(long inicio, int usos) {}

  private final int maximo;
  private final Clock clock;
  private final ConcurrentHashMap<Long, Ventana> ventanas = new ConcurrentHashMap<>();
  private volatile long ultimaLimpieza;

  LimitePorMinuto(int maximo, Clock clock) {
    this.maximo = maximo;
    this.clock = clock;
  }

  /** Cuenta una acción del usuario; falso si ya hizo {@code maximo} en su minuto. */
  boolean turno(Long userId) {
    long ahora = clock.millis();
    if (ventanas.size() >= MAX_VIGILADOS && !ventanas.containsKey(userId)) {
      if (ahora - ultimaLimpieza >= 1000) {
        ultimaLimpieza = ahora;
        ventanas.values().removeIf(v -> v.inicio() <= ahora - VENTANA_MS);
      }
      // Lleno de ventanas vivas: se deja pasar sin vigilar en vez de bloquear a usuarios que no han hecho nada.
      if (ventanas.size() >= MAX_VIGILADOS) return true;
    }
    Ventana v = ventanas.compute(userId, (k, a) -> a == null || a.inicio() <= ahora - VENTANA_MS
      ? new Ventana(ahora, 1) : new Ventana(a.inicio(), Math.min(a.usos() + 1, maximo + 1)));
    return v.usos() <= maximo;
  }
}
