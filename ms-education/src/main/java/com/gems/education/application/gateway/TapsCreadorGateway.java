package com.gems.education.application.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Persistencia de los Taps de creador (tabla taps_creador). Toda lectura y escritura del dueño lleva el dueño en el
 * WHERE; las consultas de revisión (superadmin) y la pública llevan el estado en el WHERE, nunca se filtra después.
 */
public interface TapsCreadorGateway {

  /**
   * Una fila de taps_creador tal como la ve su dueño. {@code borrador} es el JSON canónico que se edita; {@code publicado}
   * la copia congelada que se juega (null si nunca se publicó).
   */
  record Tap(String id, Long ownerId, String institutionId, String titulo, String visibilidad, String estado,
             int version, int revision, String borrador, String publicado, String motivo, Instant creado,
             Instant actualizado, Instant publicadoEn) {}

  /** Lo único que sale al público de un Tap con enlace: su id, la versión y la copia congelada. Nunca el dueño. */
  record Publicado(String id, int version, String publicado, Instant publicadoEn) {}

  /** Inserta; vacío si el id ya existía (el caso de uso reintenta con otro id). */
  Mono<Tap> crear(Tap tap);

  /** El Tap vivo de ese dueño, o vacío (también cuando existe pero es de otro: nunca se distingue). */
  Mono<Tap> buscar(String id, Long ownerId);

  /** Los Taps vivos de ese dueño, del más reciente al más antiguo. */
  Flux<Tap> listar(Long ownerId, String estado);

  /**
   * Guarda el borrador si el Tap es de ese dueño, está vivo y su revisión sigue siendo {@code revisionEsperada}
   * (null: sin comprobar). Devuelve la fila nueva o vacío si nada cambió.
   */
  Mono<Tap> actualizar(String id, Long ownerId, Integer revisionEsperada, String titulo, String borrador, Instant ahora);

  /** Borrado lógico; verdadero si había un Tap vivo de ese dueño. */
  Mono<Boolean> borrar(String id, Long ownerId, Instant ahora);

  /** Taps vivos (sin borrar) del dueño. */
  Mono<Long> contarVivos(Long ownerId);

  /** Taps creados por el dueño desde {@code desde}, borrados incluidos (borrar no devuelve cuota). */
  Mono<Long> contarCreadosDesde(Long ownerId, Instant desde);

  // ── Estados (A3) ──────────────────────────────────────────────────────────

  /**
   * Publica directo: {@code publicado} pasa a ser la copia congelada, {@code version} sube, {@code estado='publicado'} y
   * la visibilidad es la pedida. Solo si el Tap es del dueño, está vivo, no está en revisión y (si viene) su revisión es
   * {@code revisionEsperada}. Vacío si nada cambió.
   */
  Mono<Tap> publicar(String id, Long ownerId, Integer revisionEsperada, String visibilidad, String publicado, Instant ahora);

  /**
   * Manda el borrador a la cola: {@code estado='en_revision'}, visibilidad {@code enlace}. Solo desde borrador, rechazado o
   * publicado. La copia publicada sigue en el aire solo si ya estaba con enlace; una copia privada (nunca revisada) se
   * anula para que no se vuelva pública al entrar en revisión. Vacío si nada cambió.
   */
  Mono<Tap> enviarRevision(String id, Long ownerId, Integer revisionEsperada, Instant ahora);

  /** Vuelve a borrador privado (retira el enlace, también si estaba en revisión). Vacío si ya era borrador o es ajeno. */
  Mono<Tap> despublicar(String id, Long ownerId, Instant ahora);

  /** La cola de revisión: Taps vivos en {@code en_revision}, del más antiguo al más nuevo. */
  Flux<Tap> listarEnRevision();

  /** Un Tap vivo en {@code en_revision}, o vacío (sin dueño: lo usa solo la revisión del superadmin). */
  Mono<Tap> buscarEnRevision(String id);

  /**
   * Aprueba: copia {@code publicado} (el borrador revalidado), sube {@code version}, {@code estado='publicado'} con enlace.
   * Solo si sigue en revisión y su revisión es {@code revisionEsperada} (así una edición posterior a lo que vio el revisor
   * no se publica). Vacío si nada cambió.
   */
  Mono<Tap> aprobar(String id, int revisionEsperada, String publicado, Long revisorId, Instant ahora);

  /**
   * Rechaza o pide cambios: {@code estado='rechazado'} con motivo. Sin copia aprobada previa pasa a privado; con ella
   * (re-revisión) la copia sigue publicada hasta nueva decisión. Solo si sigue en revisión.
   */
  Mono<Tap> rechazar(String id, int revisionEsperada, String motivo, Long revisorId, Instant ahora);

  /**
   * La copia pública de un Tap con enlace, o vacío. Público = vivo, con copia publicada, visibilidad {@code enlace} y en
   * estado {@code publicado} (o {@code en_revision} de una versión nueva: la aprobada sigue). Rechazado o borrador: nada.
   */
  Mono<Publicado> buscarPublicado(String id);
}
