package com.gems.education.application;

import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsGateway;
import com.gems.education.application.gateway.TapsGateway.Identidad;
import com.gems.education.application.gateway.TapsGateway.Jugador;
import com.gems.education.application.gateway.TapsGateway.Posicion;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Rankings de Taps (MVP): registra partidas, calcula su XP y responde la clasificación de la semana y del día.
 *
 * <ul>
 *   <li>Solo cuentan los Taps del banco ({@link TapsBanco}); el tema y el total de preguntas deben ser los del Tap.</li>
 *   <li>XP = aciertos × 10, más 20 si la partida es perfecta. Desde la 4.ª vez que la misma identidad juega
 *       el mismo Tap en la semana, la partida da el 25 % (redondeo hacia abajo).</li>
 *   <li>Una partida más rápida que 1,5 s por pregunta se guarda pero no da XP (no se rechaza: la web sigue su
 *       flujo normal y el ranking no se altera).</li>
 *   <li>Semana: lunes 00:00 a lunes 00:00 hora de Colombia. «Hoy»: el día calendario de Colombia.</li>
 *   <li>Empates: a igual XP va primero quien llegó antes a ese XP (su última partida con XP es más antigua);
 *       los puestos no se comparten.</li>
 *   <li>Percentil: % de los demás jugadores del periodo (sin contarte) con XP estrictamente menor, entero
 *       0–100 redondeado hacia abajo. Si juegas solo, 100.</li>
 *   <li>Identidad: con sesión válida la partida suma al usuario; sin sesión, al dispositivo (jugadorId). Un
 *       dispositivo compartido nunca mezcla personas: las partidas anónimas y las de cada usuario van por separado.
 *       El usuario tiene su propio alias (no hereda el del dispositivo, para que no haya dos filas con el mismo
 *       alias).</li>
 * </ul>
 */
public class TapsRankingUseCase {
  public static final ZoneId ZONA = ZoneId.of("America/Bogota");
  /** Categorías de la web (TAP_TOPICS). El API se despliega antes que la web cuando entra una nueva. */
  public static final Set<String> TEMAS = Set.of("minecraft", "logica", "cultura", "tecnologia", "historias");
  public static final String SEMANA = "semana";
  public static final String HOY = "hoy";

  static final int XP_POR_ACIERTO = 10;
  static final int BONO_PERFECTO = 20;
  static final int REPETICIONES_COMPLETAS = 3;
  static final long MS_MINIMOS_POR_PREGUNTA = 1500;
  static final Duration ESPERA_ENTRE_PARTIDAS = Duration.ofSeconds(10);
  static final int MAX_PARTIDAS_POR_HORA = 60;
  static final int LIMITE_POR_DEFECTO = 20;
  static final int LIMITE_MAXIMO = 50;

  static final Pattern TAP_ID = Pattern.compile("^[a-z0-9-]{1,60}$");
  private static final Pattern UUID_TEXTO =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  /** Listas fijas para los alias (sustantivos masculinos para que concuerden con cualquier adjetivo). */
  static final List<String> SUSTANTIVOS = List.of("Creeper", "Ghast", "Aldeano", "Golem", "Zorro", "Ajolote",
    "Pistón", "Lobo", "Panda", "Loro", "Delfín", "Búho", "Gato", "Cofre", "Yunque", "Faro", "Cohete", "Cristal",
    "Bloque", "Dragón", "Halcón", "Castor", "Pingüino", "Tigre", "Mapache", "Colibrí");
  static final List<String> ADJETIVOS = List.of("Veloz", "Sabio", "Valiente", "Astuto", "Audaz", "Feliz", "Ágil",
    "Genial", "Brillante", "Alegre", "Leal", "Fuerte", "Tenaz", "Sagaz", "Curioso", "Noble", "Tranquilo", "Atento",
    "Ingenioso", "Rápido");

  public record Partida(String jugadorId, String tapId, String tema, Integer aciertos, Integer total, Long duracionMs) {}

  public record Semana(int puesto, int jugadores, int xp) {}

  public record Hoy(int percentil, int jugadores) {}

  public record Resultado(String alias, int xpGanado, Semana semana, Hoy hoy) {}

  public record Entrada(int puesto, String alias, int xp, boolean esTu) {}

  public record Yo(int puesto, int xp, int percentil, String alias) {}

  public record Ranking(String periodo, String desde, String hasta, int jugadores, List<Entrada> top, Yo yo) {}

  /** Rango [desde, hasta) de un periodo. */
  public record Rango(Instant desde, Instant hasta) {}

  private final TapsGateway gateway;
  private final TapsBanco banco;
  private final Clock clock;
  private final Random random;

  public TapsRankingUseCase(TapsGateway gateway, TapsBanco banco) {
    this(gateway, banco, Clock.systemUTC(), new Random());
  }

  public TapsRankingUseCase(TapsGateway gateway, TapsBanco banco, Clock clock, Random random) {
    this.gateway = gateway;
    this.banco = banco;
    this.clock = clock;
    this.random = random;
  }

  // ── Reglas puras ───────────────────────────────────────────────────────────

  /** XP de una partida antes de la regla de repetición; 0 si fue más rápida de lo posible. */
  static int xpBase(int aciertos, int total, long duracionMs) {
    if (duracionMs < total * MS_MINIMOS_POR_PREGUNTA) return 0;
    return aciertos * XP_POR_ACIERTO + (aciertos == total ? BONO_PERFECTO : 0);
  }

  /** Aplica la regla de repetición: {@code jugadasAntes} partidas previas del mismo Tap en la semana. */
  static int xpConRepeticion(int xp, long jugadasAntes) {
    return jugadasAntes >= REPETICIONES_COMPLETAS ? xp / 4 : xp;
  }

  /** % de los demás jugadores ({@code jugadores} te incluye) con XP estrictamente menor; 100 si juegas solo. */
  static int percentil(int menores, int jugadores) {
    return jugadores <= 1 ? 100 : (int) ((long) menores * 100 / (jugadores - 1));
  }

  static Rango semana(Instant ahora) {
    ZonedDateTime lunes = ahora.atZone(ZONA).toLocalDate()
      .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(ZONA);
    return new Rango(lunes.toInstant(), lunes.plusWeeks(1).toInstant());
  }

  static Rango hoy(Instant ahora) {
    ZonedDateTime inicio = ahora.atZone(ZONA).toLocalDate().atStartOfDay(ZONA);
    return new Rango(inicio.toInstant(), inicio.plusDays(1).toInstant());
  }

  String nuevoAlias() {
    return SUSTANTIVOS.get(random.nextInt(SUSTANTIVOS.size())) + " "
      + ADJETIVOS.get(random.nextInt(ADJETIVOS.size())) + " " + (100 + random.nextInt(900));
  }

  // ── Registrar una partida ──────────────────────────────────────────────────

  /** Guarda la partida y responde el XP ganado y cómo va la identidad. {@code usuarioId} es null sin sesión. */
  public Mono<Resultado> registrar(Partida p, Long usuarioId) {
    UUID jugadorId;
    try {
      jugadorId = validar(p);
    } catch (IllegalArgumentException e) {
      return Mono.error(e);
    }
    Instant ahora = clock.instant();
    Rango semana = semana(ahora);
    Rango hoy = hoy(ahora);
    int base = xpBase(p.aciertos(), p.total(), p.duracionMs());

    return dispositivo(jugadorId, usuarioId, ahora)
      .flatMap(d -> gateway.reservarTurno(jugadorId, ahora, ESPERA_ENTRE_PARTIDAS, MAX_PARTIDAS_POR_HORA)
        .defaultIfEmpty(false)
        .flatMap(ok -> ok ? identidad(d, usuarioId, ahora)
          : Mono.error(new TapsLimiteException("Demasiadas partidas seguidas: espera unos segundos"))))
      .flatMap(id -> gateway.repeticiones(id, p.tapId(), semana.desde(), semana.hasta()).defaultIfEmpty(0L)
        .map(antes -> xpConRepeticion(base, antes))
        .flatMap(xp -> gateway.guardarPartida(id, p.tapId(), p.tema(), p.aciertos(), p.total(), xp, p.duracionMs(), ahora)
          .then(Mono.zip(posicionOVacia(id, semana, null), posicionOVacia(id, hoy, null)))
          .map(pos -> {
            Posicion s = pos.getT1().orElse(new Posicion(1, xp, 1, 0, id.alias()));
            Posicion d = pos.getT2().orElse(new Posicion(1, xp, 1, 0, id.alias()));
            return new Resultado(id.alias(), xp, new Semana(s.puesto(), s.jugadores(), s.xp()),
              new Hoy(percentil(d.menores(), d.jugadores()), d.jugadores()));
          })));
  }

  private Mono<Optional<Posicion>> posicionOVacia(Identidad id, Rango r, String tema) {
    return gateway.posicion(id, r.desde(), r.hasta(), tema).map(Optional::of).defaultIfEmpty(Optional.empty());
  }

  /** El dispositivo; se crea con un alias nuevo (el de sus partidas anónimas) la primera vez que se ve. */
  private Mono<Jugador> dispositivo(UUID jugadorId, Long usuarioId, Instant ahora) {
    return gateway.buscarJugador(jugadorId)
      .switchIfEmpty(Mono.defer(() -> gateway.crearJugador(new Jugador(jugadorId, usuarioId, nuevoAlias()), ahora)));
  }

  /**
   * Con quién compite la partida. Sin sesión: el dispositivo, con su alias. Con sesión: el usuario, con su propio
   * alias (se le asigna uno nuevo la primera vez). Aunque el dispositivo esté vinculado a otro usuario, la partida
   * suma solo al usuario de la sesión.
   */
  private Mono<Identidad> identidad(Jugador d, Long usuarioId, Instant ahora) {
    if (usuarioId == null) return Mono.just(new Identidad(d.jugadorId(), null, d.alias()));
    Mono<Void> vincular = d.usuarioId() == null ? gateway.vincularDispositivo(d.jugadorId(), usuarioId) : Mono.empty();
    return gateway.aliasDeUsuario(usuarioId)
      .switchIfEmpty(Mono.defer(() -> gateway.crearUsuario(usuarioId, nuevoAlias(), ahora)))
      .flatMap(alias -> vincular.thenReturn(new Identidad(d.jugadorId(), usuarioId, alias)));
  }

  // ── Consultar el ranking ───────────────────────────────────────────────────

  /**
   * Top del periodo y, si se sabe quién pregunta, su posición: con sesión, la del usuario; sin sesión, la de las
   * partidas anónimas del dispositivo {@code jugadorId}.
   */
  public Mono<Ranking> ranking(String periodo, String tema, String jugadorId, Integer limite, Long usuarioId) {
    String p = periodo == null || periodo.isBlank() ? SEMANA : periodo.trim().toLowerCase();
    if (!SEMANA.equals(p) && !HOY.equals(p)) return Mono.error(new IllegalArgumentException("periodo debe ser semana u hoy"));
    String t = tema == null || tema.isBlank() ? null : tema.trim().toLowerCase();
    if (t != null && !TEMAS.contains(t)) return Mono.error(new IllegalArgumentException("tema no válido"));
    UUID id = null;
    if (jugadorId != null && !jugadorId.isBlank()) {
      if (!UUID_TEXTO.matcher(jugadorId.trim()).matches()) return Mono.error(new IllegalArgumentException("jugadorId no es un UUID"));
      id = UUID.fromString(jugadorId.trim());
    }
    int n = limite == null ? LIMITE_POR_DEFECTO : Math.max(1, Math.min(LIMITE_MAXIMO, limite));
    Rango r = SEMANA.equals(p) ? semana(clock.instant()) : hoy(clock.instant());
    UUID dispositivo = id;

    Mono<Optional<Identidad>> quien;
    if (usuarioId != null) {
      quien = gateway.aliasDeUsuario(usuarioId).map(a -> Optional.of(new Identidad(dispositivo, usuarioId, a)))
        .defaultIfEmpty(Optional.empty());
    } else if (dispositivo != null) {
      quien = gateway.buscarJugador(dispositivo).map(j -> Optional.of(new Identidad(dispositivo, null, j.alias())))
        .defaultIfEmpty(Optional.empty());
    } else {
      quien = Mono.just(Optional.empty());
    }
    return quien.flatMap(q -> {
      String clave = q.map(Identidad::clave).orElse(null);
      Mono<List<TapsGateway.Fila>> filas = gateway.top(r.desde(), r.hasta(), t, n).collectList();
      Mono<Optional<Posicion>> pos = q.isEmpty() ? Mono.just(Optional.empty()) : posicionOVacia(q.get(), r, t);
      return Mono.zip(filas, pos).map(z -> {
        List<Entrada> top = z.getT1().stream()
          .map(f -> new Entrada(f.puesto(), f.alias(), f.xp(), f.clave().equals(clave))).toList();
        int jugadores = z.getT2().map(Posicion::jugadores)
          .orElse(z.getT1().isEmpty() ? 0 : z.getT1().getFirst().jugadores());
        Yo mio = z.getT2().map(x -> new Yo(x.puesto(), x.xp(), percentil(x.menores(), x.jugadores()), x.alias())).orElse(null);
        return new Ranking(p, r.desde().toString(), r.hasta().toString(), jugadores, top, mio);
      });
    });
  }

  // ── Validación ─────────────────────────────────────────────────────────────

  private UUID validar(Partida p) {
    if (p == null) throw new IllegalArgumentException("Falta la partida");
    if (p.jugadorId() == null || !UUID_TEXTO.matcher(p.jugadorId()).matches()) {
      throw new IllegalArgumentException("jugadorId debe ser un UUID");
    }
    if (p.tapId() == null || !TAP_ID.matcher(p.tapId()).matches()) {
      throw new IllegalArgumentException("tapId no válido");
    }
    if (p.tema() == null || !TEMAS.contains(p.tema())) throw new IllegalArgumentException("tema no válido");
    if (p.total() == null || p.total() < 1 || p.total() > 10) throw new IllegalArgumentException("total debe estar entre 1 y 10");
    if (p.aciertos() == null || p.aciertos() < 0 || p.aciertos() > p.total()) {
      throw new IllegalArgumentException("aciertos debe estar entre 0 y total");
    }
    if (p.duracionMs() == null || p.duracionMs() < 0) throw new IllegalArgumentException("duracionMs no válido");
    TapsBanco.Tap tap = banco.buscar(p.tapId()).orElseThrow(() -> new IllegalArgumentException("Tap desconocido"));
    if (!tap.tema().equals(p.tema())) throw new IllegalArgumentException("El tema no es el del Tap");
    if (tap.total() != p.total()) throw new IllegalArgumentException("total no es el número de preguntas del Tap");
    return UUID.fromString(p.jugadorId());
  }
}
