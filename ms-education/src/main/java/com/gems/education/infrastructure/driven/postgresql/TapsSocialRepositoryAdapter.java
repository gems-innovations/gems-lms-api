package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.TapsSocialGateway;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

/** Likes, comentarios y reportes de Taps (taps_like, taps_comentario, taps_comentario_reporte). */
@Repository
public class TapsSocialRepositoryAdapter implements TapsSocialGateway {
  private final DatabaseClient db;

  public TapsSocialRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<Cifras> cifras(String tapId, String quien) {
    DatabaseClient.GenericExecuteSpec spec = db.sql("SELECT "
        + "(SELECT COUNT(*) FROM taps_like WHERE tap_id = :tap) AS likes, "
        + "(SELECT COUNT(*) FROM taps_comentario WHERE tap_id = :tap AND NOT oculto) AS comentarios, "
        + "EXISTS (SELECT 1 FROM taps_like WHERE tap_id = :tap AND quien = :quien) AS yo")
      .bind("tap", tapId);
    spec = quien == null ? spec.bindNull("quien", String.class) : spec.bind("quien", quien);
    return spec.map(row -> new Cifras(row.get("likes", Long.class), row.get("comentarios", Long.class),
      Boolean.TRUE.equals(row.get("yo", Boolean.class)))).one();
  }

  @Override
  public Mono<Void> darLike(String tapId, String quien, Instant ahora) {
    return db.sql("INSERT INTO taps_like (tap_id, quien, creado) VALUES (:tap, :quien, :ahora) ON CONFLICT DO NOTHING")
      .bind("tap", tapId).bind("quien", quien).bind("ahora", ahora)
      .then();
  }

  @Override
  public Mono<Void> quitarLike(String tapId, String quien) {
    return db.sql("DELETE FROM taps_like WHERE tap_id = :tap AND quien = :quien")
      .bind("tap", tapId).bind("quien", quien)
      .then();
  }

  @Override
  public Flux<Comentario> comentarios(String tapId, Long antesDe, int limite) {
    DatabaseClient.GenericExecuteSpec spec = db.sql("SELECT c.id, c.usuario_id, u.alias, c.texto, c.creado "
        + "FROM taps_comentario c JOIN taps_usuario u ON u.usuario_id = c.usuario_id "
        + "WHERE c.tap_id = :tap AND NOT c.oculto AND (CAST(:antes AS BIGINT) IS NULL OR c.id < :antes) "
        + "ORDER BY c.id DESC LIMIT :limite")
      .bind("tap", tapId).bind("limite", limite);
    spec = antesDe == null ? spec.bindNull("antes", Long.class) : spec.bind("antes", antesDe);
    return spec.map(TapsSocialRepositoryAdapter::toComentario).all();
  }

  @Override
  public Mono<Comentario> comentar(String tapId, long usuarioId, String texto, Instant ahora) {
    return db.sql("WITH c AS (INSERT INTO taps_comentario (tap_id, usuario_id, texto, creado) "
        + "VALUES (:tap, :u, :texto, :ahora) RETURNING id, usuario_id, texto, creado) "
        + "SELECT c.id, c.usuario_id, u.alias, c.texto, c.creado FROM c JOIN taps_usuario u ON u.usuario_id = c.usuario_id")
      .bind("tap", tapId).bind("u", usuarioId).bind("texto", texto).bind("ahora", ahora)
      .map(TapsSocialRepositoryAdapter::toComentario).one();
  }

  @Override
  public Mono<Boolean> borrar(String tapId, long comentarioId, long usuarioId) {
    return db.sql("DELETE FROM taps_comentario WHERE id = :id AND tap_id = :tap AND usuario_id = :u")
      .bind("id", comentarioId).bind("tap", tapId).bind("u", usuarioId)
      .fetch().rowsUpdated()
      .map(n -> n > 0);
  }

  /** Un comentario que no existe (o de otro Tap) no anota nada; el UPDATE cuenta los reportes ya guardados. */
  @Override
  public Mono<Void> reportar(String tapId, long comentarioId, long usuarioId, int umbral, Instant ahora) {
    return db.sql("INSERT INTO taps_comentario_reporte (comentario_id, usuario_id, creado) "
        + "SELECT id, :u, :ahora FROM taps_comentario WHERE id = :id AND tap_id = :tap ON CONFLICT DO NOTHING")
      .bind("id", comentarioId).bind("tap", tapId).bind("u", usuarioId).bind("ahora", ahora)
      .then()
      .then(db.sql("UPDATE taps_comentario SET oculto = true WHERE id = :id AND NOT oculto "
          + "AND (SELECT COUNT(*) FROM taps_comentario_reporte WHERE comentario_id = :id) >= :umbral")
        .bind("id", comentarioId).bind("umbral", umbral)
        .then());
  }

  /** Mismo UPDATE condicional que {@link TapsRepositoryAdapter#reservarTurno}, sobre la fila del usuario. */
  @Override
  public Mono<Boolean> reservarComentario(long usuarioId, Instant ahora, Duration espera, int maximoPorHora) {
    return db.sql("UPDATE taps_usuario SET ultimo_comentario = :ahora, "
        + "ventana_comentarios = CASE WHEN ventana_comentarios_inicio IS NULL OR ventana_comentarios_inicio <= :haceHora "
        + "THEN 1 ELSE ventana_comentarios + 1 END, "
        + "ventana_comentarios_inicio = CASE WHEN ventana_comentarios_inicio IS NULL OR ventana_comentarios_inicio <= :haceHora "
        + "THEN :ahora ELSE ventana_comentarios_inicio END "
        + "WHERE usuario_id = :u "
        + "AND (ultimo_comentario IS NULL OR ultimo_comentario <= :haceEspera) "
        + "AND (ventana_comentarios_inicio IS NULL OR ventana_comentarios_inicio <= :haceHora OR ventana_comentarios < :maximo)")
      .bind("ahora", ahora).bind("haceHora", ahora.minus(Duration.ofHours(1))).bind("haceEspera", ahora.minus(espera))
      .bind("maximo", maximoPorHora).bind("u", usuarioId)
      .fetch().rowsUpdated()
      .map(n -> n > 0);
  }

  private static Comentario toComentario(io.r2dbc.spi.Readable row) {
    return new Comentario(row.get("id", Long.class), row.get("usuario_id", Long.class), row.get("alias", String.class),
      row.get("texto", String.class), row.get("creado", OffsetDateTime.class).toInstant());
  }
}
