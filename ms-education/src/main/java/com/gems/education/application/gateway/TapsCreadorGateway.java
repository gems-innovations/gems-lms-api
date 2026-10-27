package com.gems.education.application.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/** Persistencia de los Taps de creador (tabla taps_creador). Toda lectura y escritura lleva el dueño en el WHERE. */
public interface TapsCreadorGateway {

  /** Una fila de taps_creador tal como la ve su dueño. {@code borrador} es el JSON canónico del validador. */
  record Tap(String id, Long ownerId, String institutionId, String titulo, String visibilidad, String estado,
             int version, int revision, String borrador, String motivo, Instant creado, Instant actualizado,
             Instant publicadoEn) {}

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
}
