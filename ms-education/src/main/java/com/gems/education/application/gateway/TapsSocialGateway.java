package com.gems.education.application.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Likes y comentarios de Taps (tablas taps_like, taps_comentario y taps_comentario_reporte). {@code quien} es la
 * clave de identidad del ranking: {@code u<usuario>} con sesión o {@code j<dispositivo>} sin sesión.
 */
public interface TapsSocialGateway {

  /** Un comentario visible, con el alias de Taps de su autor. */
  record Comentario(long id, long usuarioId, String alias, String texto, Instant creado) {}

  /** Likes del Tap, comentarios visibles y si {@code quien} (puede ser null) le dio like. */
  record Cifras(long likes, long comentarios, boolean yoLike) {}

  Mono<Cifras> cifras(String tapId, String quien);

  /** Idempotente: si ya existía no hace nada. */
  Mono<Void> darLike(String tapId, String quien, Instant ahora);

  /** Idempotente: si no existía no hace nada. */
  Mono<Void> quitarLike(String tapId, String quien);

  /** Comentarios visibles del Tap, del más nuevo al más viejo, con id menor que {@code antesDe} (null = desde el último). */
  Flux<Comentario> comentarios(String tapId, Long antesDe, int limite);

  /** El usuario debe tener fila en taps_usuario (su alias). */
  Mono<Comentario> comentar(String tapId, long usuarioId, String texto, Instant ahora);

  /** Borra el comentario solo si es de ese usuario; falso si no existe o es de otra persona. */
  Mono<Boolean> borrar(String tapId, long comentarioId, long usuarioId);

  /** Anota el reporte (uno por usuario) y oculta el comentario al llegar a {@code umbral} reportes distintos. */
  Mono<Void> reportar(String tapId, long comentarioId, long usuarioId, int umbral, Instant ahora);

  /**
   * Reserva, de forma atómica, el turno para comentar: falso si el usuario comentó hace menos de {@code espera} o ya
   * hizo {@code maximoPorHora} comentarios en la ventana de una hora en curso.
   */
  Mono<Boolean> reservarComentario(long usuarioId, Instant ahora, Duration espera, int maximoPorHora);
}
