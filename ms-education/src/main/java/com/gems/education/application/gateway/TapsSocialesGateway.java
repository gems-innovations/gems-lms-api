package com.gems.education.application.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Cifras sociales de los Taps: «me gusta» (taps_me_gusta), comentarios (taps_comentario, con sus reportes en
 * taps_comentario_reporte) y vistas de taps_estadistica. Me gusta y comentarios son de usuarios con sesión.
 */
public interface TapsSocialesGateway {

  /** Un comentario guardado. {@code userId} es interno: nunca sale en una respuesta. */
  record Comentario(long id, String tapId, Long userId, String autor, String texto, Instant creado, boolean oculto) {}

  // ── Me gusta ───────────────────────────────────────────────────────────────

  /** Pone el me gusta del usuario; si ya estaba, no cambia nada. */
  Mono<Void> ponerMeGusta(String tapId, Long userId);

  /** Quita el me gusta del usuario; si no estaba, no cambia nada. */
  Mono<Void> quitarMeGusta(String tapId, Long userId);

  /** Total de me gusta por Tap (los Taps sin ninguno no aparecen). */
  Mono<Map<String, Long>> meGusta(Collection<String> tapIds);

  /** De esos Taps, a cuáles les dio me gusta el usuario. */
  Mono<Set<String>> meGustaDe(Long userId, Collection<String> tapIds);

  /** Vistas por Tap en los días [desde, hasta] (los Taps sin vistas no aparecen). */
  Mono<Map<String, Long>> vistas(Collection<String> tapIds, LocalDate desde, LocalDate hasta);

  // ── Comentarios ────────────────────────────────────────────────────────────

  /** Comentarios visibles (ni borrados ni ocultos) por Tap. */
  Mono<Map<String, Long>> comentarios(Collection<String> tapIds);

  Mono<Comentario> comentar(String tapId, Long userId, String autor, String texto);

  /** Comentarios visibles del Tap con id menor que {@code antes}, del más nuevo al más viejo. */
  Flux<Comentario> pagina(String tapId, long antes, int limite);

  /** El comentario si no está borrado (puede estar oculto). */
  Mono<Comentario> buscarComentario(long id);

  /** Comentarios que escribió el usuario desde {@code desde} (también los que luego borró). */
  Mono<Long> comentariosDesde(Long userId, Instant desde);

  /** Borrado lógico. */
  Mono<Void> borrarComentario(long id);

  /** Guarda el reporte del usuario; falso si ya lo había reportado. */
  Mono<Boolean> reportar(long id, Long userId);

  /** Oculta el comentario si tiene al menos {@code minimo} reportes y el superadmin no lo revisó. */
  Mono<Void> ocultarSiReportado(long id, int minimo);

  /** Decisión del superadmin: oculta o restaura y lo marca revisado (los reportes ya no lo ocultan). */
  Mono<Void> cambiarOculto(long id, boolean oculto);
}
