package com.gems.education.application;

import com.gems.education.application.exceptions.TapsComentarioException;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsSocialesGateway;
import com.gems.education.application.gateway.TapsSocialesGateway.Comentario;
import com.gems.education.application.gateway.UserDirectory;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.ForbiddenException;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Comentarios de Taps, solo con sesión (decisión del dueño, 29-oct: hay menores, así que ni leerlos es público).
 *
 * <ul>
 *   <li>El autor se muestra como nombre + inicial del apellido («Ana R.»), calculado al escribir; nunca correo ni id.
 *       Un nombre que no sea solo letras (un apodo con enlace o teléfono, la parte local de un correo) o con palabras
 *       bloqueadas se muestra como {@value #SIN_NOMBRE}.</li>
 *   <li>Texto de 1 a {@value #MAX_TEXTO} caracteres con el filtro de los Taps de creador ({@link TapsPayloadValidador}: sin
 *       HTML, enlaces, correos, teléfonos ni palabras bloqueadas); si no pasa, 422 con el motivo.</li>
 *   <li>Máximo {@value #MAX_POR_MINUTO} por minuto (en memoria, atómico) y {@value #MAX_POR_DIA} en 24 h (en la base,
 *       contando los borrados) por usuario.</li>
 *   <li>Borra el autor o el SUPER_ADMIN (borrado lógico). Un reporte por usuario; con {@value #REPORTES_PARA_OCULTAR}
 *       se oculta solo, salvo que el SUPER_ADMIN ya lo haya revisado. No cuentan los reportes propios, de ocultos ni de
 *       Taps que ya no están publicados. El SUPER_ADMIN oculta o restaura.</li>
 *   <li>Páginas de {@value #POR_PAGINA}, del más nuevo al más viejo; {@code siguiente} es el cursor de la próxima.</li>
 * </ul>
 */
public class TapsComentariosUseCase {
  static final int POR_PAGINA = 20;
  static final int MAX_TEXTO = 200;
  static final int MAX_POR_MINUTO = 10;
  static final int MAX_POR_DIA = 100;
  static final int REPORTES_PARA_OCULTAR = 3;
  static final String SIN_NOMBRE = "Usuario";
  /** Solo letras (con apóstrofo o guion por dentro): ni dígitos, ni puntos, ni arrobas. */
  private static final Pattern NOMBRE = Pattern.compile("^\\p{L}[\\p{L}'-]{0,29}$");

  public record Vista(long id, String autor, String texto, Instant creado, boolean mio) {}

  public record Pagina(List<Vista> comentarios, Long siguiente) {}

  private final TapsSocialesGateway gateway;
  private final TapsRegistrados registrados;
  private final TapsPayloadValidador validador;
  private final UserDirectory users;
  private final Clock clock;
  private final LimitePorMinuto porMinuto;

  public TapsComentariosUseCase(TapsSocialesGateway gateway, TapsRegistrados registrados, TapsPayloadValidador validador,
                                UserDirectory users, Clock clock) {
    this.gateway = gateway;
    this.registrados = registrados;
    this.validador = validador;
    this.users = users;
    this.clock = clock;
    this.porMinuto = new LimitePorMinuto(MAX_POR_MINUTO, clock);
  }

  /** «Ana R.»: el primer nombre y la inicial del apellido; {@value #SIN_NOMBRE} si el nombre no es solo letras. */
  String autor(String nombre, String apellido) {
    String n = primera(nombre), a = primera(apellido);
    if (!NOMBRE.matcher(n).matches() || validador.tieneBloqueada(n)) return SIN_NOMBRE;
    return a.isEmpty() || !Character.isLetter(a.codePointAt(0)) ? n
      : n + " " + new String(Character.toChars(a.codePointAt(0))).toUpperCase(Locale.ROOT) + ".";
  }

  public Mono<Pagina> leer(AuthenticatedUser yo, String tapId, Long antes) {
    if (antes != null && antes < 1) return Mono.error(new IllegalArgumentException("antes no válido"));
    long cursor = antes == null ? Long.MAX_VALUE : antes;
    return tap(tapId).flatMap(id -> gateway.pagina(id, cursor, POR_PAGINA + 1).collectList()).map(l -> {
      boolean hayMas = l.size() > POR_PAGINA;
      List<Comentario> pagina = hayMas ? l.subList(0, POR_PAGINA) : l;
      return new Pagina(pagina.stream().map(c -> vista(c, yo)).toList(), hayMas ? pagina.getLast().id() : null);
    });
  }

  /** Publica el comentario. Si ms-auth no responde se devuelve su error (503): no se guarda un nombre provisional. */
  public Mono<Vista> comentar(AuthenticatedUser yo, String tapId, String texto) {
    String limpio;
    try {
      limpio = validador.comentarioLimpio(texto, MAX_TEXTO, "el comentario");
    } catch (IllegalArgumentException e) {
      return Mono.error(TapsComentarioException.invalido(e.getMessage()));
    }
    Instant ahora = clock.instant();
    return tap(tapId).flatMap(id -> {
      if (!porMinuto.turno(yo.userId())) return Mono.error(new TapsLimiteException("Espera un minuto antes de comentar otra vez"));
      return gateway.comentariosDesde(yo.userId(), ahora.minus(Duration.ofDays(1))).flatMap(n -> n >= MAX_POR_DIA
        ? Mono.<Comentario>error(new TapsLimiteException("Ya escribiste " + MAX_POR_DIA + " comentarios hoy; sigue mañana"))
        : users.find(yo.userId()).map(p -> autor(p.firstName(), p.lastName())).defaultIfEmpty(SIN_NOMBRE)
          .flatMap(autor -> gateway.comentar(id, yo.userId(), autor, limpio)));
    }).map(c -> vista(c, yo));
  }

  public Mono<Void> borrar(AuthenticatedUser yo, long id) {
    return existente(id).flatMap(c -> yo.isUser(c.userId()) || yo.isSuperAdmin()
      ? gateway.borrarComentario(id)
      : Mono.error(new ForbiddenException("Solo quien escribió el comentario puede borrarlo")));
  }

  /** Responde igual (sin efecto) si el comentario es propio, ya está oculto o su Tap ya no está publicado. */
  public Mono<Void> reportar(AuthenticatedUser yo, long id) {
    return existente(id)
      .filter(c -> !c.oculto() && !yo.isUser(c.userId()))
      .filterWhen(c -> registrados.buscar(c.tapId()).hasElement())
      .flatMap(c -> gateway.reportar(id, yo.userId()))
      .flatMap(nuevo -> nuevo ? gateway.ocultarSiReportado(id, REPORTES_PARA_OCULTAR) : Mono.empty());
  }

  public Mono<Void> cambiarEstado(AuthenticatedUser yo, long id, Boolean oculto) {
    if (!yo.isSuperAdmin()) return Mono.error(new ForbiddenException("Solo el superadmin modera comentarios"));
    if (oculto == null) return Mono.error(new IllegalArgumentException("Falta oculto"));
    return existente(id).flatMap(c -> gateway.cambiarOculto(id, oculto));
  }

  private Mono<Comentario> existente(long id) {
    return gateway.buscarComentario(id).switchIfEmpty(Mono.error(TapsComentarioException::noExiste));
  }

  private Mono<String> tap(String tapId) {
    if (tapId == null || !TapsRankingUseCase.TAP_ID.matcher(tapId).matches()) {
      return Mono.error(new IllegalArgumentException("tapId no válido"));
    }
    return registrados.buscar(tapId).map(TapsBanco.Tap::id)
      .switchIfEmpty(Mono.error(new IllegalArgumentException("Tap desconocido")));
  }

  private static Vista vista(Comentario c, AuthenticatedUser yo) {
    return new Vista(c.id(), c.autor(), c.texto(), c.creado(), yo.isUser(c.userId()));
  }

  private static String primera(String s) {
    if (s == null) return "";
    String t = s.strip();
    int espacio = t.indexOf(' ');
    return espacio < 0 ? t : t.substring(0, espacio);
  }
}
