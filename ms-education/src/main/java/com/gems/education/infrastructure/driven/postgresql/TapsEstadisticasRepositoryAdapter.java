package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.TapsEstadisticasGateway;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/** Contadores anónimos de Taps (tabla taps_estadistica): una fila por Tap, día, evento y pregunta. */
@Repository
public class TapsEstadisticasRepositoryAdapter implements TapsEstadisticasGateway {
  private final DatabaseClient db;

  public TapsEstadisticasRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<Void> sumar(String tapId, LocalDate dia, String evento, int pregunta) {
    return db.sql("INSERT INTO taps_estadistica (tap_id, dia, evento, pregunta, cantidad) VALUES (:t, :d, :e, :p, 1) "
        + "ON CONFLICT (tap_id, dia, evento, pregunta) DO UPDATE SET cantidad = taps_estadistica.cantidad + 1")
      .bind("t", tapId).bind("d", dia).bind("e", evento).bind("p", (short) pregunta)
      .then();
  }

  @Override
  public Flux<Fila> leer(LocalDate desde, LocalDate hasta) {
    return db.sql("SELECT tap_id, evento, pregunta, SUM(cantidad) AS cantidad FROM taps_estadistica "
        + "WHERE dia BETWEEN :desde AND :hasta GROUP BY tap_id, evento, pregunta")
      .bind("desde", desde).bind("hasta", hasta)
      .map(row -> new Fila(row.get("tap_id", String.class), row.get("evento", String.class),
        row.get("pregunta", Short.class).intValue(), row.get("cantidad", Long.class)))
      .all();
  }
}