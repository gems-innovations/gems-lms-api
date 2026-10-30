package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.TapsSocialesGateway;
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Me gusta (taps_me_gusta), comentarios (taps_comentario, taps_comentario_reporte) y vistas de taps_estadistica. */
@Repository
public class TapsSocialesRepositoryAdapter implements TapsSocialesGateway {
  private static final String COMENTARIO = "id, tap_id, user_id, autor, texto, creado, oculto";
  private final DatabaseClient db;

  public TapsSocialesRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<Void> ponerMeGusta(String tapId, Long userId) {
    return db.sql("INSERT INTO taps_me_gusta (tap_id, user_id) VALUES (:t, :u) ON CONFLICT DO NOTHING")
      .bind("t", tapId).bind("u", userId).then();
  }

  @Override
  public Mono<Void> quitarMeGusta(String tapId, Long userId) {
    return db.sql("DELETE FROM taps_me_gusta WHERE tap_id = :t AND user_id = :u")
      .bind("t", tapId).bind("u", userId).then();
  }

  @Override
  public Mono<Map<String, Long>> meGusta(Collection<String> tapIds) {
    return contarPorTap("SELECT tap_id, COUNT(*) AS n FROM taps_me_gusta WHERE tap_id = ANY(:ids) GROUP BY tap_id", tapIds);
  }

  @Override
  public Mono<Set<String>> meGustaDe(Long userId, Collection<String> tapIds) {
    if (tapIds.isEmpty()) return Mono.just(Set.of());
    return db.sql("SELECT tap_id FROM taps_me_gusta WHERE user_id = :u AND tap_id = ANY(:ids)")
      .bind("u", userId).bind("ids", tapIds.toArray(String[]::new))
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

  @Override
  public Mono<Map<String, Long>> comentarios(Collection<String> tapIds) {
    return contarPorTap("SELECT tap_id, COUNT(*) AS n FROM taps_comentario "
      + "WHERE tap_id = ANY(:ids) AND borrado_en IS NULL AND NOT oculto GROUP BY tap_id", tapIds);
  }

  @Override
  public Mono<Comentario> comentar(String tapId, Long userId, String autor, String texto) {
    return db.sql("INSERT INTO taps_comentario (tap_id, user_id, autor, texto) VALUES (:t, :u, :a, :x) RETURNING " + COMENTARIO)
      .bind("t", tapId).bind("u", userId).bind("a", autor).bind("x", texto)
      .map(TapsSocialesRepositoryAdapter::comentario).one();
  }

  @Override
  public Flux<Comentario> pagina(String tapId, long antes, int limite) {
    return db.sql("SELECT " + COMENTARIO + " FROM taps_comentario WHERE tap_id = :t AND borrado_en IS NULL AND NOT oculto "
        + "AND id < :antes ORDER BY id DESC LIMIT :n")
      .bind("t", tapId).bind("antes", antes).bind("n", limite)
      .map(TapsSocialesRepositoryAdapter::comentario).all();
  }

  @Override
  public Mono<Comentario> buscarComentario(long id) {
    return db.sql("SELECT " + COMENTARIO + " FROM taps_comentario WHERE id = :id AND borrado_en IS NULL")
      .bind("id", id).map(TapsSocialesRepositoryAdapter::comentario).one();
  }

  @Override
  public Mono<Long> comentariosDesde(Long userId, Instant desde) {
    return db.sql("SELECT COUNT(*) AS n FROM taps_comentario WHERE user_id = :u AND creado >= :desde")
      .bind("u", userId).bind("desde", desde).map(row -> row.get("n", Long.class)).one();
  }

  @Override
  public Mono<Void> borrarComentario(long id) {
    return db.sql("UPDATE taps_comentario SET borrado_en = now() WHERE id = :id AND borrado_en IS NULL").bind("id", id).then();
  }

  @Override
  public Mono<Boolean> reportar(long id, Long userId) {
    return db.sql("INSERT INTO taps_comentario_reporte (comentario_id, user_id) VALUES (:id, :u) ON CONFLICT DO NOTHING")
      .bind("id", id).bind("u", userId).fetch().rowsUpdated().map(n -> n > 0);
  }

  @Override
  public Mono<Void> ocultarSiReportado(long id, int minimo) {
    return db.sql("UPDATE taps_comentario SET oculto = true WHERE id = :id AND NOT oculto AND NOT revisado "
        + "AND (SELECT COUNT(*) FROM taps_comentario_reporte WHERE comentario_id = :id) >= :min")
      .bind("id", id).bind("min", minimo).then();
  }

  @Override
  public Mono<Void> cambiarOculto(long id, boolean oculto) {
    return db.sql("UPDATE taps_comentario SET oculto = :o, revisado = true WHERE id = :id").bind("o", oculto).bind("id", id).then();
  }

  private Mono<Map<String, Long>> contarPorTap(String sql, Collection<String> tapIds) {
    if (tapIds.isEmpty()) return Mono.just(Map.of());
    return db.sql(sql).bind("ids", tapIds.toArray(String[]::new))
      .map(row -> Map.entry(row.get("tap_id", String.class), row.get("n", Long.class)))
      .all().collectMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  private static Comentario comentario(Readable row) {
    return new Comentario(row.get("id", Long.class), row.get("tap_id", String.class), row.get("user_id", Long.class),
      row.get("autor", String.class), row.get("texto", String.class), row.get("creado", OffsetDateTime.class).toInstant(),
      Boolean.TRUE.equals(row.get("oculto", Boolean.class)));
  }
}
