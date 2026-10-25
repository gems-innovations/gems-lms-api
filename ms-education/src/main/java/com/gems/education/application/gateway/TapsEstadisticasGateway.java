package com.gems.education.application.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Contadores anónimos por Tap, día, evento y pregunta (tabla taps_estadistica). No guarda a quién pertenece cada
 * evento: solo cuántos hubo. Sirve para decidir con datos qué Tap mostrar primero y dónde abandona la gente.
 */
public interface TapsEstadisticasGateway {

  /** Una suma ya agregada del rango: cuántas veces ocurrió {@code evento} en {@code pregunta} de {@code tapId}. */
  record Fila(String tapId, String evento, int pregunta, long cantidad) {}

  /** Suma 1 al contador (creándolo si no existe); atómico aunque lleguen eventos simultáneos. */
  Mono<Void> sumar(String tapId, LocalDate dia, String evento, int pregunta);

  /** Los contadores de los días [desde, hasta], agrupados por Tap, evento y pregunta. */
  Flux<Fila> leer(LocalDate desde, LocalDate hasta);
}