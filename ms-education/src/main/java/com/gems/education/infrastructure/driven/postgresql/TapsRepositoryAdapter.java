package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.TapsGateway;
import io.r2dbc.spi.Readable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Dispositivos, usuarios y partidas de Taps (tablas taps_jugador, taps_usuario y taps_partida). Los rankings se
 * calculan con una sola consulta: suma de XP por identidad en el rango, ordenada por XP y, a igual XP, por quién
 * llegó antes.
 */
@Repository
public class TapsRepositoryAdapter implements TapsGateway {
  /** Identidad con la que compite cada partida: su usuario si se jugó con sesión, si no el dispositivo. */
  private static final String CLAVE =
    "CASE WHEN p.usuario_id IS NOT NULL THEN 'u' || p.usuario_id::text ELSE 'j' || p.jugador_id::text END";

  private final DatabaseClient db;

  public TapsRepositoryAdapter(DatabaseClient db) {
    this.db = db;
  }

  @Override
  public Mono<Jugador> buscarJugador(UUID jugadorId) {
    return db.sql("SELECT jugador_id, usuario_id, alias FROM taps_jugador WHERE jugador_id = :id")
      .bind("id", jugadorId)
      .map(TapsRepositoryAdapter::toJugador).one();
  }

  @Override
  public Mono<Jugador> crearJugador(Jugador jugador, Instant creado) {
    // Si otro envío simultáneo ya lo creó, se devuelve el existente (y su alias).
    DatabaseClient.GenericExecuteSpec spec = db.sql("INSERT INTO taps_jugador (jugador_id, usuario_id, alias, creado) "
        + "VALUES (:id, :u, :alias, :creado) "
        + "ON CONFLICT (jugador_id) DO UPDATE SET jugador_id = EXCLUDED.jugador_id "
        + "RETURNING jugador_id, usuario_id, alias")
      .bind("id", jugador.jugadorId()).bind("alias", jugador.alias()).bind("creado", creado);
    spec = jugador.usuarioId() == null ? spec.bindNull("u", Long.class) : spec.bind("u", jugador.usuarioId());
    return spec.map(TapsRepositoryAdapter::toJugador).one();
  }

  @Override
  public Mono<Void> vincularDispositivo(UUID jugadorId, Long usuarioId) {
    return db.sql("UPDATE taps_jugador SET usuario_id = :u WHERE jugador_id = :id AND usuario_id IS NULL")
      .bind("u", usuarioId).bind("id", jugadorId)
      .then();
  }

  @Override
  public Mono<String> aliasDeUsuario(Long usuarioId) {
    return db.sql("SELECT alias FROM taps_usuario WHERE usuario_id = :u")
      .bind("u", usuarioId)
      .map(row -> row.get("alias", String.class)).one();
  }

  @Override
  public Mono<String> crearUsuario(Long usuarioId, String alias, Instant creado) {
    return db.sql("INSERT INTO taps_usuario (usuario_id, alias, creado) VALUES (:u, :alias, :creado) "
        + "ON CONFLICT (usuario_id) DO UPDATE SET usuario_id = EXCLUDED.usuario_id RETURNING alias")
      .bind("u", usuarioId).bind("alias", alias).bind("creado", creado)
      .map(row -> row.get("alias", String.class)).one();
  }

  /**
   * Un solo UPDATE condicional sobre la fila del dispositivo: Postgres la bloquea y, si otra petición la cambió
   * mientras tanto, vuelve a evaluar el WHERE con los valores nuevos, así que dos envíos simultáneos no pueden
   * tomar el mismo turno. La ventana de una hora es fija: empieza con la primera partida y se renueva al vencer.
   */
  @Override
  public Mono<Boolean> reservarTurno(UUID jugadorId, Instant ahora, Duration espera, int maximoPorHora) {
    return db.sql("UPDATE taps_jugador SET ultima_partida = :ahora, "
        + "ventana_partidas = CASE WHEN ventana_inicio IS NULL OR ventana_inicio <= :haceHora THEN 1 "
        + "ELSE ventana_partidas + 1 END, "
        + "ventana_inicio = CASE WHEN ventana_inicio IS NULL OR ventana_inicio <= :haceHora THEN :ahora "
        + "ELSE ventana_inicio END "
        + "WHERE jugador_id = :id "
        + "AND (ultima_partida IS NULL OR ultima_partida <= :haceEspera) "
        + "AND (ventana_inicio IS NULL OR ventana_inicio <= :haceHora OR ventana_partidas < :maximo)")
      .bind("ahora", ahora).bind("haceHora", ahora.minus(Duration.ofHours(1))).bind("haceEspera", ahora.minus(espera))
      .bind("maximo", maximoPorHora).bind("id", jugadorId)
      .fetch().rowsUpdated()
      .map(n -> n > 0);
  }

  @Override
  public Mono<Long> repeticiones(Identidad identidad, String tapId, Instant desde, Instant hasta) {
    String quien = identidad.usuarioId() != null ? "usuario_id = :quien" : "jugador_id = :quien AND usuario_id IS NULL";
    DatabaseClient.GenericExecuteSpec spec = db.sql("SELECT COUNT(*) AS n FROM taps_partida "
        + "WHERE " + quien + " AND tap_id = :tap AND creada >= :desde AND creada < :hasta")
      .bind("tap", tapId).bind("desde", desde).bind("hasta", hasta);
    spec = identidad.usuarioId() != null ? spec.bind("quien", identidad.usuarioId()) : spec.bind("quien", identidad.jugadorId());
    return spec.map(row -> row.get("n", Long.class)).one();
  }

  @Override
  public Mono<Void> guardarPartida(Identidad identidad, String tapId, String tema, int aciertos, int total, int xp,
                                   long duracionMs, Instant creada) {
    DatabaseClient.GenericExecuteSpec spec = db.sql("INSERT INTO taps_partida "
        + "(jugador_id, usuario_id, tap_id, tema, aciertos, total, xp, duracion_ms, creada) "
        + "VALUES (:id, :u, :tap, :tema, :aciertos, :total, :xp, :duracion, :creada)")
      .bind("id", identidad.jugadorId()).bind("tap", tapId).bind("tema", tema).bind("aciertos", aciertos)
      .bind("total", total).bind("xp", xp).bind("duracion", duracionMs).bind("creada", creada);
    spec = identidad.usuarioId() == null ? spec.bindNull("u", Long.class) : spec.bind("u", identidad.usuarioId());
    return spec.then();
  }

  @Override
  public Flux<Fila> top(Instant desde, Instant hasta, String tema, int limite) {
    return rango(clasificacion(tema) + "SELECT clave, alias, xp, puesto, jugadores FROM r WHERE puesto <= :limite ORDER BY puesto",
        desde, hasta, tema)
      .bind("limite", limite)
      .map(row -> new Fila(entero(row, "puesto"), row.get("alias", String.class), entero(row, "xp"),
        row.get("clave", String.class), entero(row, "jugadores")))
      .all();
  }

  @Override
  public Mono<Posicion> posicion(Identidad identidad, Instant desde, Instant hasta, String tema) {
    return rango(clasificacion(tema) + "SELECT r.puesto, r.xp, r.jugadores, r.alias, "
          + "(SELECT COUNT(*) FROM t WHERE t.xp < r.xp) AS menores FROM r WHERE r.clave = :clave",
        desde, hasta, tema)
      .bind("clave", identidad.clave())
      .map(row -> new Posicion(entero(row, "puesto"), entero(row, "xp"), entero(row, "jugadores"),
        entero(row, "menores"), row.get("alias", String.class)))
      .one();
  }

  /**
   * t: XP de cada identidad en el rango, su alias (el del usuario o el del dispositivo) y cuándo alcanzó ese XP
   * (su última partida con XP; si ninguna dio XP, la primera). r: t numerada (puestos sin compartir) con el total.
   */
  private static String clasificacion(String tema) {
    return "WITH t AS (SELECT " + CLAVE + " AS clave, MIN(COALESCE(u.alias, j.alias)) AS alias, SUM(p.xp) AS xp, "
      + "COALESCE(MAX(p.creada) FILTER (WHERE p.xp > 0), MIN(p.creada)) AS alcanzado "
      + "FROM taps_partida p JOIN taps_jugador j ON j.jugador_id = p.jugador_id "
      + "LEFT JOIN taps_usuario u ON u.usuario_id = p.usuario_id "
      + "WHERE p.creada >= :desde AND p.creada < :hasta" + (tema != null ? " AND p.tema = :tema" : "")
      + " GROUP BY 1), "
      + "r AS (SELECT t.*, ROW_NUMBER() OVER (ORDER BY t.xp DESC, t.alcanzado ASC, t.clave ASC) AS puesto, "
      + "COUNT(*) OVER () AS jugadores FROM t) ";
  }

  private DatabaseClient.GenericExecuteSpec rango(String sql, Instant desde, Instant hasta, String tema) {
    DatabaseClient.GenericExecuteSpec spec = db.sql(sql).bind("desde", desde).bind("hasta", hasta);
    return tema != null ? spec.bind("tema", tema) : spec;
  }

  private static int entero(Readable row, String columna) {
    Number n = row.get(columna, Number.class);
    return n == null ? 0 : n.intValue();
  }

  private static Jugador toJugador(Readable row) {
    return new Jugador(row.get("jugador_id", UUID.class), row.get("usuario_id", Long.class), row.get("alias", String.class));
  }
}
