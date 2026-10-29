package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.TapsSocialesGateway;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** «Me gusta» anónimos (taps_me_gusta: uno por Tap y jugador) y vistas agregadas de taps_estadistica. */
@Repository
public class TapsSocialesRepositoryAdapter implements TapsSocialesGateway {
  private final DatabaseClient db;

  public TapsSocialesRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<Void> ponerMeGusta(String tapId, UUID jugadorId) {
    return db.sql("INSERT INTO taps_me_gusta (tap_id, jugador_id) VALUES (:t, :j) ON CONFLICT DO NOTHING")
      .bind("t", tapId).bind("j", jugadorId).then();
  }

  @Override
  public Mono<Void> quitarMeGusta(String tapId, UUID jugadorId) {
    return db.sql("DELETE FROM taps_me_gusta WHERE tap_id = :t AND jugador_id = :j")
      .bind("t", tapId).bind("j", jugadorId).then();
  }

  @Override
  public Mono<Map<String, Long>> meGusta(Collection<String> tapIds) {
    if (tapIds.isEmpty()) return Mono.just(Map.of());
    return db.sql("SELECT tap_id, COUNT(*) AS n FROM taps_me_gusta WHERE tap_id = ANY(:ids) GROUP BY tap_id")
      .bind("ids", tapIds.toArray(String[]::new))
      .map(row -> Map.entry(row.get("tap_id", String.class), row.get("n", Long.class)))
      .all().collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  @Override
  public Mono<Set<String>> meGustaDe(UUID jugadorId, Collection<String> tapIds) {
    if (tapIds.isEmpty()) return Mono.just(Set.of());
    return db.sql("SELECT tap_id FROM taps_me_gusta WHERE jugador_id = :j AND tap_id = ANY(:ids)")
      .bind("j", jugadorId).bind("ids", tapIds.toArray(String[]::new))
      .map(row -> row.get("tap_id", String.class))
      .all().collect(HashSet::new, Set::add);
  }

  @Override
  public Mono<Map<String, Long>> vistas(Collection<String> tapIds, LocalDate desde, LocalDate hasta) {
    if (tapIds.isEmpty()) return Mono.just(Map.of());
    return db.sql("SELECT tap_id, SUM(cantidad) AS n FROM taps_estadistica "
        + "WHERE evento = 'vista' AND dia BETWEEN :desde AND :hasta AND tap_id = ANY(:ids) GROUP BY tap_id")
      .bind("desde", desde).bind("hasta", hasta).bind("ids", tapIds.toArray(String[]::new))
      .map(row -> Map.entry(row.get("tap_id", String.class), ((Number) row.get("n")).longValue()))
      .all().collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }
}
