package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.TapsCreadorGateway;
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Tabla taps_creador. Toda consulta por id lleva también {@code owner_id = :owner AND borrado_en IS NULL}: el dueño se
 * comprueba en la base, no después, y una fila ajena simplemente no aparece.
 */
@Repository
public class TapsCreadorRepositoryAdapter implements TapsCreadorGateway {
  private static final String COLUMNAS = "id, owner_id, institution_id, titulo, visibilidad, estado, version, revision, "
    + "borrador, motivo, creado, actualizado, publicado_en";

  private final DatabaseClient db;

  public TapsCreadorRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<Tap> crear(Tap tap) {
    DatabaseClient.GenericExecuteSpec spec = db.sql("INSERT INTO taps_creador (id, owner_id, institution_id, titulo, "
        + "visibilidad, estado, version, revision, borrador, creado, actualizado) "
        + "VALUES (:id, :owner, :inst, :titulo, :vis, :estado, 0, 0, :borrador, :creado, :creado) "
        + "ON CONFLICT (id) DO NOTHING RETURNING " + COLUMNAS)
      .bind("id", tap.id()).bind("owner", tap.ownerId()).bind("titulo", tap.titulo()).bind("vis", tap.visibilidad())
      .bind("estado", tap.estado()).bind("borrador", tap.borrador()).bind("creado", tap.creado());
    spec = tap.institutionId() == null ? spec.bindNull("inst", String.class) : spec.bind("inst", tap.institutionId());
    return spec.map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Mono<Tap> buscar(String id, Long ownerId) {
    return db.sql("SELECT " + COLUMNAS + " FROM taps_creador WHERE id = :id AND owner_id = :owner AND borrado_en IS NULL")
      .bind("id", id).bind("owner", ownerId)
      .map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Flux<Tap> listar(Long ownerId, String estado) {
    String sql = "SELECT " + COLUMNAS + " FROM taps_creador WHERE owner_id = :owner AND borrado_en IS NULL"
      + (estado != null ? " AND estado = :estado" : "") + " ORDER BY actualizado DESC, id";
    DatabaseClient.GenericExecuteSpec spec = db.sql(sql).bind("owner", ownerId);
    if (estado != null) spec = spec.bind("estado", estado);
    return spec.map(TapsCreadorRepositoryAdapter::toTap).all();
  }

  @Override
  public Mono<Tap> actualizar(String id, Long ownerId, Integer revisionEsperada, String titulo, String borrador, Instant ahora) {
    String sql = "UPDATE taps_creador SET titulo = :titulo, borrador = :borrador, revision = revision + 1, actualizado = :ahora "
      + "WHERE id = :id AND owner_id = :owner AND borrado_en IS NULL"
      + (revisionEsperada != null ? " AND revision = :rev" : "") + " RETURNING " + COLUMNAS;
    DatabaseClient.GenericExecuteSpec spec = db.sql(sql)
      .bind("titulo", titulo).bind("borrador", borrador).bind("ahora", ahora).bind("id", id).bind("owner", ownerId);
    if (revisionEsperada != null) spec = spec.bind("rev", revisionEsperada);
    return spec.map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Mono<Boolean> borrar(String id, Long ownerId, Instant ahora) {
    return db.sql("UPDATE taps_creador SET borrado_en = :ahora, actualizado = :ahora "
        + "WHERE id = :id AND owner_id = :owner AND borrado_en IS NULL")
      .bind("ahora", ahora).bind("id", id).bind("owner", ownerId)
      .fetch().rowsUpdated().map(n -> n > 0);
  }

  @Override
  public Mono<Long> contarVivos(Long ownerId) {
    return db.sql("SELECT COUNT(*) AS n FROM taps_creador WHERE owner_id = :owner AND borrado_en IS NULL")
      .bind("owner", ownerId).map(row -> row.get("n", Long.class)).one();
  }

  @Override
  public Mono<Long> contarCreadosDesde(Long ownerId, Instant desde) {
    return db.sql("SELECT COUNT(*) AS n FROM taps_creador WHERE owner_id = :owner AND creado >= :desde")
      .bind("owner", ownerId).bind("desde", desde).map(row -> row.get("n", Long.class)).one();
  }

  private static Tap toTap(Readable row) {
    return new Tap(row.get("id", String.class), row.get("owner_id", Long.class), row.get("institution_id", String.class),
      row.get("titulo", String.class), row.get("visibilidad", String.class), row.get("estado", String.class),
      entero(row, "version"), entero(row, "revision"), row.get("borrador", String.class), row.get("motivo", String.class),
      instante(row, "creado"), instante(row, "actualizado"), instante(row, "publicado_en"));
  }

  private static int entero(Readable row, String columna) {
    Number n = row.get(columna, Number.class);
    return n == null ? 0 : n.intValue();
  }

  private static Instant instante(Readable row, String columna) {
    OffsetDateTime t = row.get(columna, OffsetDateTime.class);
    return t == null ? null : t.withOffsetSameInstant(ZoneOffset.UTC).toInstant();
  }
}
