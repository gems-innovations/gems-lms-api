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
 * Tabla taps_creador. Toda consulta del dueño por id lleva también {@code owner_id = :owner AND borrado_en IS NULL}: el
 * dueño se comprueba en la base, no después, y una fila ajena simplemente no aparece. Las transiciones de estado llevan
 * el estado de partida en el WHERE, así dos peticiones a la vez no publican dos veces ni aprueban lo ya retirado.
 */
@Repository
public class TapsCreadorRepositoryAdapter implements TapsCreadorGateway {
  private static final String COLUMNAS = "id, owner_id, institution_id, titulo, visibilidad, estado, version, revision, "
    + "borrador, publicado, motivo, creado, actualizado, publicado_en";
  private static final String VIVO = " AND borrado_en IS NULL";
  /**
   * Lo que se puede jugar sin cuenta: con copia publicada, con enlace, y no retirado. Un {@code rechazado} entra solo si
   * conserva una copia aprobada con enlace (re-revisión que pidió cambios): la primera revisión rechazada queda en privado.
   */
  private static final String PUBLICO = " AND publicado IS NOT NULL AND visibilidad = 'enlace' AND estado IN ('publicado', 'en_revision', 'rechazado')";

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
    return db.sql("SELECT " + COLUMNAS + " FROM taps_creador WHERE id = :id AND owner_id = :owner" + VIVO)
      .bind("id", id).bind("owner", ownerId)
      .map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Flux<Tap> listar(Long ownerId, String estado) {
    String sql = "SELECT " + COLUMNAS + " FROM taps_creador WHERE owner_id = :owner" + VIVO
      + (estado != null ? " AND estado = :estado" : "") + " ORDER BY actualizado DESC, id";
    DatabaseClient.GenericExecuteSpec spec = db.sql(sql).bind("owner", ownerId);
    if (estado != null) spec = spec.bind("estado", estado);
    return spec.map(TapsCreadorRepositoryAdapter::toTap).all();
  }

  @Override
  public Mono<Tap> actualizar(String id, Long ownerId, Integer revisionEsperada, String titulo, String borrador, Instant ahora) {
    String sql = "UPDATE taps_creador SET titulo = :titulo, borrador = :borrador, revision = revision + 1, actualizado = :ahora "
      + "WHERE id = :id AND owner_id = :owner" + VIVO
      + (revisionEsperada != null ? " AND revision = :rev" : "") + " RETURNING " + COLUMNAS;
    DatabaseClient.GenericExecuteSpec spec = db.sql(sql)
      .bind("titulo", titulo).bind("borrador", borrador).bind("ahora", ahora).bind("id", id).bind("owner", ownerId);
    if (revisionEsperada != null) spec = spec.bind("rev", revisionEsperada);
    return spec.map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Mono<Boolean> borrar(String id, Long ownerId, Instant ahora) {
    return db.sql("UPDATE taps_creador SET borrado_en = :ahora, actualizado = :ahora "
        + "WHERE id = :id AND owner_id = :owner" + VIVO)
      .bind("ahora", ahora).bind("id", id).bind("owner", ownerId)
      .fetch().rowsUpdated().map(n -> n > 0);
  }

  @Override
  public Mono<Long> contarVivos(Long ownerId) {
    return db.sql("SELECT COUNT(*) AS n FROM taps_creador WHERE owner_id = :owner" + VIVO)
      .bind("owner", ownerId).map(row -> row.get("n", Long.class)).one();
  }

  @Override
  public Mono<Long> contarCreadosDesde(Long ownerId, Instant desde) {
    return db.sql("SELECT COUNT(*) AS n FROM taps_creador WHERE owner_id = :owner AND creado >= :desde")
      .bind("owner", ownerId).bind("desde", desde).map(row -> row.get("n", Long.class)).one();
  }

  // ── Estados (A3) ──────────────────────────────────────────────────────────

  @Override
  public Mono<Tap> publicar(String id, Long ownerId, Integer revisionEsperada, String visibilidad, String publicado, Instant ahora) {
    String sql = "UPDATE taps_creador SET estado = 'publicado', visibilidad = :vis, publicado = :publicado, "
      + "version = version + 1, revision = revision + 1, publicado_en = :ahora, actualizado = :ahora, motivo = NULL "
      + "WHERE id = :id AND owner_id = :owner" + VIVO + " AND estado <> 'en_revision'"
      + (revisionEsperada != null ? " AND revision = :rev" : "") + " RETURNING " + COLUMNAS;
    DatabaseClient.GenericExecuteSpec spec = db.sql(sql)
      .bind("vis", visibilidad).bind("publicado", publicado).bind("ahora", ahora).bind("id", id).bind("owner", ownerId);
    if (revisionEsperada != null) spec = spec.bind("rev", revisionEsperada);
    return spec.map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Mono<Tap> enviarRevision(String id, Long ownerId, Integer revisionEsperada, Instant ahora) {
    // Solo una copia que ya estaba con enlace (revisada o de docente) sigue en el aire mientras se revisa la nueva; una
    // copia publicada en privado (nunca revisada) no puede volverse pública por pasar a en_revision.
    String sql = "UPDATE taps_creador SET estado = 'en_revision', revision = revision + 1, actualizado = :ahora, motivo = NULL, "
      + "publicado = CASE WHEN visibilidad = 'enlace' THEN publicado ELSE NULL END, visibilidad = 'enlace' "
      + "WHERE id = :id AND owner_id = :owner" + VIVO + " AND estado IN ('borrador', 'rechazado', 'publicado')"
      + (revisionEsperada != null ? " AND revision = :rev" : "") + " RETURNING " + COLUMNAS;
    DatabaseClient.GenericExecuteSpec spec = db.sql(sql).bind("ahora", ahora).bind("id", id).bind("owner", ownerId);
    if (revisionEsperada != null) spec = spec.bind("rev", revisionEsperada);
    return spec.map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Mono<Tap> despublicar(String id, Long ownerId, Instant ahora) {
    return db.sql("UPDATE taps_creador SET estado = 'borrador', visibilidad = 'private', revision = revision + 1, "
        + "actualizado = :ahora WHERE id = :id AND owner_id = :owner" + VIVO + " AND estado <> 'borrador' RETURNING " + COLUMNAS)
      .bind("ahora", ahora).bind("id", id).bind("owner", ownerId)
      .map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Flux<Tap> listarEnRevision() {
    return db.sql("SELECT " + COLUMNAS + " FROM taps_creador WHERE estado = 'en_revision'" + VIVO + " ORDER BY actualizado, id")
      .map(TapsCreadorRepositoryAdapter::toTap).all();
  }

  @Override
  public Mono<Tap> buscarEnRevision(String id) {
    return db.sql("SELECT " + COLUMNAS + " FROM taps_creador WHERE id = :id AND estado = 'en_revision'" + VIVO)
      .bind("id", id).map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Mono<Tap> aprobar(String id, int revisionEsperada, String publicado, Long revisorId, Instant ahora) {
    return db.sql("UPDATE taps_creador SET estado = 'publicado', visibilidad = 'enlace', publicado = :publicado, "
        + "version = version + 1, revision = revision + 1, publicado_en = :ahora, actualizado = :ahora, motivo = NULL, "
        + "revisado_por = :revisor, revisado_en = :ahora "
        + "WHERE id = :id AND estado = 'en_revision' AND revision = :rev" + VIVO + " RETURNING " + COLUMNAS)
      .bind("publicado", publicado).bind("ahora", ahora).bind("revisor", revisorId).bind("id", id).bind("rev", revisionEsperada)
      .map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Mono<Tap> rechazar(String id, int revisionEsperada, String motivo, Long revisorId, Instant ahora) {
    // Decisión del dueño (27-oct): si ya había una copia aprobada en el aire (re-revisión), sigue publicada hasta nueva
    // decisión; solo la primera revisión (sin copia) pasa a privado. En en_revision la copia solo sobrevive con enlace.
    return db.sql("UPDATE taps_creador SET estado = 'rechazado', "
        + "visibilidad = CASE WHEN publicado IS NOT NULL THEN visibilidad ELSE 'private' END, motivo = :motivo, "
        + "revision = revision + 1, actualizado = :ahora, revisado_por = :revisor, revisado_en = :ahora "
        + "WHERE id = :id AND estado = 'en_revision' AND revision = :rev" + VIVO + " RETURNING " + COLUMNAS)
      .bind("motivo", motivo).bind("ahora", ahora).bind("revisor", revisorId).bind("id", id).bind("rev", revisionEsperada)
      .map(TapsCreadorRepositoryAdapter::toTap).one();
  }

  @Override
  public Mono<Publicado> buscarPublicado(String id) {
    return db.sql("SELECT id, version, publicado, publicado_en FROM taps_creador WHERE id = :id" + VIVO + PUBLICO)
      .bind("id", id)
      .map(row -> new Publicado(row.get("id", String.class), entero(row, "version"), row.get("publicado", String.class),
        instante(row, "publicado_en")))
      .one();
  }

  private static Tap toTap(Readable row) {
    return new Tap(row.get("id", String.class), row.get("owner_id", Long.class), row.get("institution_id", String.class),
      row.get("titulo", String.class), row.get("visibilidad", String.class), row.get("estado", String.class),
      entero(row, "version"), entero(row, "revision"), row.get("borrador", String.class), row.get("publicado", String.class),
      row.get("motivo", String.class), instante(row, "creado"), instante(row, "actualizado"), instante(row, "publicado_en"));
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
