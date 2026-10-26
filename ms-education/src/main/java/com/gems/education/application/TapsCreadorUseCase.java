package com.gems.education.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.gems.education.application.exceptions.TapsCreadorConflictoException;
import com.gems.education.application.exceptions.TapsCreadorNotFoundException;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsCreadorGateway;
import com.gems.education.application.gateway.TapsCreadorGateway.Tap;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.web.ClientQuota;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Borradores de Taps del creador autenticado (PR-10 A2). Propiedad estricta: cada consulta lleva el dueño, así que un Tap
 * ajeno es indistinguible de uno inexistente (404). Cuotas ({@code creador.md} §5.3): 30 Taps vivos por cuenta, 30
 * creados por día (borrar no devuelve cuota) y 60 guardados por minuto por usuario (autoguardado). El contenido pasa
 * siempre por {@link TapsPayloadValidador} y se guarda en su forma canónica.
 *
 * <p>Publicar, revisar e importar el borrador de la solicitud quedan para A3 y A4.
 */
public class TapsCreadorUseCase {
  public static final int MAX_VIVOS = 30;
  public static final int MAX_CREADOS_POR_DIA = 30;
  public static final int MAX_GUARDADOS_POR_MINUTO = 60;
  public static final Set<String> ESTADOS = Set.of("borrador", "en_revision", "publicado", "rechazado");
  static final int INTENTOS_ID = 5;

  private final TapsCreadorGateway taps;
  private final TapsPayloadValidador validador;
  private final Clock reloj;
  private final ClientQuota guardados;

  public TapsCreadorUseCase(TapsCreadorGateway taps, TapsPayloadValidador validador) {
    this(taps, validador, Clock.systemUTC(), new ClientQuota(MAX_GUARDADOS_POR_MINUTO, 60));
  }

  public TapsCreadorUseCase(TapsCreadorGateway taps, TapsPayloadValidador validador, Clock reloj, ClientQuota guardados) {
    this.taps = taps;
    this.validador = validador;
    this.reloj = reloj;
    this.guardados = guardados;
  }

  /** Crea un borrador privado del creador con el contenido validado; respeta las cuotas. */
  public Mono<Tap> crear(AuthenticatedUser creador, JsonNode contenido) {
    TapsPayloadValidador.Tap valido = validador.validar(contenido);
    Instant ahora = reloj.instant();
    return taps.contarVivos(creador.userId())
      .flatMap(vivos -> vivos >= MAX_VIVOS
        ? Mono.error(new TapsLimiteException("Ya tienes " + MAX_VIVOS + " Taps; borra alguno para crear otro"))
        : taps.contarCreadosDesde(creador.userId(), ahora.minus(Duration.ofDays(1))))
      .flatMap(hoy -> hoy >= MAX_CREADOS_POR_DIA
        ? Mono.error(new TapsLimiteException("Hoy ya creaste " + MAX_CREADOS_POR_DIA + " Taps; sigue mañana"))
        : insertar(creador, valido, ahora, INTENTOS_ID));
  }

  private Mono<Tap> insertar(AuthenticatedUser creador, TapsPayloadValidador.Tap valido, Instant ahora, int intentos) {
    Tap nuevo = new Tap(TapsIds.nuevo(), creador.userId(), creador.institutionId(), valido.titulo(), "private",
      "borrador", 0, 0, valido.json(), null, ahora, ahora, null);
    return taps.crear(nuevo)
      .switchIfEmpty(Mono.defer(() -> intentos > 1
        ? insertar(creador, valido, ahora, intentos - 1)
        : Mono.error(new IllegalStateException("No se pudo asignar un id al Tap"))));
  }

  /** El Tap, si es del creador; 404 si no existe o es de otra persona. */
  public Mono<Tap> buscar(AuthenticatedUser creador, String id) {
    if (!TapsIds.esDeUsuario(id)) return Mono.error(new TapsCreadorNotFoundException());
    return taps.buscar(id, creador.userId()).switchIfEmpty(Mono.error(new TapsCreadorNotFoundException()));
  }

  /** Los Taps del creador; {@code estado} filtra (400 si no es un estado conocido). */
  public Flux<Tap> listar(AuthenticatedUser creador, String estado) {
    if (estado != null && !ESTADOS.contains(estado)) return Flux.error(new IllegalArgumentException("Estado desconocido"));
    return taps.listar(creador.userId(), estado);
  }

  /**
   * Reemplaza el borrador. {@code revisionEsperada} (If-Match) opcional: si la fila ya cambió, 409. Si el Tap no es del
   * creador, 404 antes de tocar nada.
   */
  public Mono<Tap> actualizar(AuthenticatedUser creador, String id, Integer revisionEsperada, JsonNode contenido) {
    if (!TapsIds.esDeUsuario(id)) return Mono.error(new TapsCreadorNotFoundException());
    TapsPayloadValidador.Tap valido = validador.validar(contenido);
    if (!guardados.tryAcquire(String.valueOf(creador.userId())))
      return Mono.error(new TapsLimiteException("Demasiados guardados seguidos; espera un momento"));
    return taps.buscar(id, creador.userId())
      .switchIfEmpty(Mono.error(new TapsCreadorNotFoundException()))
      .flatMap(actual -> taps.actualizar(id, creador.userId(), revisionEsperada, valido.titulo(), valido.json(), reloj.instant())
        .switchIfEmpty(Mono.error(new TapsCreadorConflictoException())));
  }

  /** Borrado lógico; 404 si no existe o es de otra persona. */
  public Mono<Void> borrar(AuthenticatedUser creador, String id) {
    if (!TapsIds.esDeUsuario(id)) return Mono.error(new TapsCreadorNotFoundException());
    return taps.borrar(id, creador.userId(), reloj.instant())
      .flatMap(hecho -> hecho ? Mono.empty() : Mono.error(new TapsCreadorNotFoundException()));
  }
}
