package com.gems.education.application.gateway;

import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** «Me gusta» anónimos por Tap (tabla taps_me_gusta) y vistas de taps_estadistica para las tarjetas de la web. */
public interface TapsSocialesGateway {

  /** Pone el me gusta del jugador; si ya estaba, no cambia nada. */
  Mono<Void> ponerMeGusta(String tapId, UUID jugadorId);

  /** Quita el me gusta del jugador; si no estaba, no cambia nada. */
  Mono<Void> quitarMeGusta(String tapId, UUID jugadorId);

  /** Total de me gusta por Tap (los Taps sin ninguno no aparecen). */
  Mono<Map<String, Long>> meGusta(Collection<String> tapIds);

  /** De esos Taps, a cuáles les dio me gusta el jugador. */
  Mono<Set<String>> meGustaDe(UUID jugadorId, Collection<String> tapIds);

  /** Vistas por Tap en los días [desde, hasta] (los Taps sin vistas no aparecen). */
  Mono<Map<String, Long>> vistas(Collection<String> tapIds, LocalDate desde, LocalDate hasta);
}
