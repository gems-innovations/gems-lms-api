package com.gems.education.application;

import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsSocialGateway;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Likes y comentarios de Taps.
 *
 * <ul>
 *   <li>Like: se puede sin sesión (uno por dispositivo, con el jugadorId de la web) o con sesión (uno por usuario).
 *       Dar y quitar son idempotentes.</li>
 *   <li>Comentar y reportar exigen sesión (decisión del 29-oct): así cada comentario tiene responsable, se puede
 *       borrar el propio y los reportes no se inflan con dispositivos inventados. El autor se muestra con su alias
 *       de Taps (el del ranking), nunca con su nombre.</li>
 *   <li>Comentario: 1–200 caracteres, sin enlaces, correos, teléfonos, @usuarios ni insultos. Uno cada 15 s y
 *       hasta 10 por hora por usuario.</li>
 *   <li>Al llegar a 3 reportes de personas distintas el comentario se oculta (no se lista ni se cuenta).</li>
 *   <li>Solo Taps registrados (banco o de creador publicados); leer un Tap sin datos da ceros.</li>
 * </ul>
 */
public class TapsSocialUseCase {
  static final int TEXTO_MAXIMO = 200;
  static final Duration ESPERA_ENTRE_COMENTARIOS = Duration.ofSeconds(15);
  static final int MAX_COMENTARIOS_POR_HORA = 10;
  static final int REPORTES_PARA_OCULTAR = 3;
  static final int LIMITE_POR_DEFECTO = 20;
  static final int LIMITE_MAXIMO = 50;

  private static final Pattern TAP_ID = Pattern.compile("^[a-z0-9-]{1,60}$");
  private static final Pattern UUID_TEXTO =
    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  private static final Pattern ENLACE = Pattern.compile(
    "https?://|www\\.|\\b[a-z0-9-]+\\.(com|net|org|co|io|gg|ly|me|tv|xyz|info|app|lat|es|mx|link|site|online)\\b",
    Pattern.CASE_INSENSITIVE);
  private static final Pattern CORREO_O_USUARIO = Pattern.compile("[\\w.+-]*@[\\w-]{2,}");
  private static final Pattern TELEFONO = Pattern.compile("(\\d[\\s.-]?){7,}");
  /** shortcut: palabras exactas tras quitar tildes y leet simple; no ve variantes con letras repetidas ni separadas, ampliar si llegan reportes. */
  static final Set<String> INSULTOS = Set.of("idiota", "estupido", "estupida", "imbecil", "pendejo", "pendeja",
    "marica", "maricon", "puta", "puto", "hijueputa", "hijoputa", "gonorrea", "malparido", "malparida", "mierda",
    "verga", "zorra", "perra", "cabron", "cabrona", "huevon", "guevon", "weon", "tarado", "tarada", "retrasado",
    "retrasada", "mongolico", "subnormal", "gilipollas", "carechimba", "culero", "culera", "baboso", "babosa",
    "estupides", "fuck", "shit", "bitch", "nigga", "nigger");

  public record Social(long likes, long comentarios, boolean yoLike) {}

  public record Comentario(long id, String alias, String texto, String creado, boolean mio) {}

  /** {@code siguiente}: el cursor de la página siguiente, o null si no hay más. */
  public record Pagina(List<Comentario> comentarios, String siguiente) {}

  public record Nuevo(String texto) {}

  private final TapsSocialGateway gateway;
  private final TapsRegistrados registrados;
  private final TapsRankingUseCase ranking;
  private final Clock clock;

  public TapsSocialUseCase(TapsSocialGateway gateway, TapsRegistrados registrados, TapsRankingUseCase ranking, Clock clock) {
    this.gateway = gateway;
    this.registrados = registrados;
    this.ranking = ranking;
    this.clock = clock;
  }

  // ── Reglas puras ───────────────────────────────────────────────────────────

  /** El texto listo para guardar, o el motivo del rechazo como IllegalArgumentException. */
  static String validarTexto(String texto) {
    String t = texto == null ? "" : texto.strip().replaceAll("\\s+", " ");
    if (t.isEmpty()) throw new IllegalArgumentException("El comentario está vacío");
    if (t.length() > TEXTO_MAXIMO) throw new IllegalArgumentException("El comentario pasa de 200 caracteres");
    if (ENLACE.matcher(t).find()) throw new IllegalArgumentException("Los comentarios no pueden llevar enlaces");
    if (CORREO_O_USUARIO.matcher(t).find() || TELEFONO.matcher(t).find()) {
      throw new IllegalArgumentException("Por tu seguridad, no compartas correos, teléfonos ni usuarios");
    }
    if (tieneInsulto(t)) throw new IllegalArgumentException("Ese comentario tiene palabras que no se permiten");
    return t;
  }

  static boolean tieneInsulto(String texto) {
    String plano = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
      .replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a').replace('$', 's');
    return Arrays.stream(plano.split("[^a-zñ]+")).anyMatch(INSULTOS::contains);
  }

  /** Clave de identidad del like: el usuario si hay sesión; si no, el dispositivo; null si no hay ninguno válido. */
  static String quien(Long usuarioId, String jugadorId) {
    if (usuarioId != null) return "u" + usuarioId;
    if (jugadorId != null && UUID_TEXTO.matcher(jugadorId.trim()).matches()) return "j" + jugadorId.trim().toLowerCase();
    return null;
  }

  // ── Casos de uso ───────────────────────────────────────────────────────────

  public Mono<Social> social(String tapId, Long usuarioId, String jugadorId) {
    return idValido(tapId).then(Mono.defer(() -> leer(tapId, quien(usuarioId, jugadorId))));
  }

  public Mono<Social> like(String tapId, boolean dar, Long usuarioId, String jugadorId) {
    String q = quien(usuarioId, jugadorId);
    if (q == null) return Mono.error(new IllegalArgumentException("jugadorId debe ser un UUID"));
    Mono<Void> cambio = dar ? gateway.darLike(tapId, q, clock.instant()) : gateway.quitarLike(tapId, q);
    return registrado(tapId).then(cambio).then(Mono.defer(() -> leer(tapId, q)));
  }

  public Mono<Pagina> comentarios(String tapId, String cursor, Integer limite, Long usuarioId) {
    Long antesDe;
    try {
      antesDe = cursor == null || cursor.isBlank() ? null : Long.valueOf(cursor.trim());
    } catch (NumberFormatException e) {
      return Mono.error(new IllegalArgumentException("cursor no válido"));
    }
    int n = limite == null ? LIMITE_POR_DEFECTO : Math.max(1, Math.min(LIMITE_MAXIMO, limite));
    // Se pide uno más para saber si hay otra página.
    return idValido(tapId).thenMany(gateway.comentarios(tapId, antesDe, n + 1)).collectList().map(lista -> {
      List<Comentario> pagina = lista.stream().limit(n).map(c -> aRespuesta(c, usuarioId)).toList();
      return new Pagina(pagina, lista.size() > n ? String.valueOf(pagina.getLast().id()) : null);
    });
  }

  public Mono<Comentario> comentar(String tapId, Nuevo nuevo, long usuarioId) {
    String texto;
    try {
      texto = validarTexto(nuevo == null ? null : nuevo.texto());
    } catch (IllegalArgumentException e) {
      return Mono.error(e);
    }
    return registrado(tapId)
      .then(ranking.aliasDe(usuarioId, clock.instant()))
      .then(gateway.reservarComentario(usuarioId, clock.instant(), ESPERA_ENTRE_COMENTARIOS, MAX_COMENTARIOS_POR_HORA))
      .flatMap(ok -> ok ? gateway.comentar(tapId, usuarioId, texto, clock.instant())
        : Mono.error(new TapsLimiteException("Vas muy rápido: espera un poco antes de comentar otra vez")))
      .map(c -> aRespuesta(c, usuarioId));
  }

  public Mono<Boolean> borrar(String tapId, long comentarioId, long usuarioId) {
    return idValido(tapId).then(gateway.borrar(tapId, comentarioId, usuarioId));
  }

  public Mono<Void> reportar(String tapId, long comentarioId, long usuarioId) {
    return idValido(tapId).then(gateway.reportar(tapId, comentarioId, usuarioId, REPORTES_PARA_OCULTAR, clock.instant()));
  }

  // ── Apoyo ──────────────────────────────────────────────────────────────────

  private Mono<Social> leer(String tapId, String quien) {
    return gateway.cifras(tapId, quien).map(c -> new Social(c.likes(), c.comentarios(), c.yoLike()));
  }

  private static Comentario aRespuesta(TapsSocialGateway.Comentario c, Long usuarioId) {
    return new Comentario(c.id(), c.alias(), c.texto(), c.creado().toString(), usuarioId != null && usuarioId == c.usuarioId());
  }

  private static Mono<Void> idValido(String tapId) {
    return tapId != null && TAP_ID.matcher(tapId).matches() ? Mono.empty()
      : Mono.error(new IllegalArgumentException("tapId no válido"));
  }

  private Mono<Void> registrado(String tapId) {
    return idValido(tapId).then(registrados.buscar(tapId).hasElement())
      .flatMap(ok -> ok ? Mono.<Void>empty() : Mono.error(new IllegalArgumentException("Tap desconocido")));
  }
}
