package com.gems.education.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gems.education.application.exceptions.TapsCreadorConflictoException;
import com.gems.education.application.exceptions.TapsCreadorNotFoundException;
import com.gems.education.application.exceptions.TapsEstadoException;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsCreadorGateway;
import com.gems.education.application.gateway.TapsCreadorGateway.Tap;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.web.ClientQuota;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Taps del creador autenticado (PR-10 A2 y A3). Propiedad estricta: cada consulta del dueño lleva el dueño, así que un
 * Tap ajeno es indistinguible de uno inexistente (404). Cuotas ({@code creador.md} §5.3): 30 Taps vivos por cuenta, 30
 * creados por día (borrar no devuelve cuota), 60 guardados por minuto por usuario (autoguardado) y 5 envíos a revisión
 * por día. El contenido pasa siempre por {@link TapsPayloadValidador} y se guarda en su forma canónica.
 *
 * <p>Estados: {@code borrador → publicado} directo para docentes (INSTRUCTOR, ADMIN, SUPER_ADMIN) y para cualquier
 * publicación {@code private}; un CREATOR que pide {@code enlace} pasa a {@code en_revision} y el superadmin decide:
 * aprobar → {@code publicado}; cambios o rechazar → {@code rechazado} con motivo. Editar no saca del aire la copia
 * publicada; despublicar sí, de inmediato. Rechazar la tumba solo si no había copia aprobada previa: en una re-revisión
 * la copia aprobada sigue en el aire hasta nueva decisión. Al publicar o aprobar el borrador se valida otra vez.
 */
public class TapsCreadorUseCase {
  public static final int MAX_VIVOS = 30;
  public static final int MAX_CREADOS_POR_DIA = 30;
  public static final int MAX_GUARDADOS_POR_MINUTO = 60;
  public static final int MAX_ENVIOS_REVISION_POR_DIA = 5;
  public static final int MOTIVO_MAX = 300;
  public static final Set<String> ESTADOS = Set.of("borrador", "en_revision", "publicado", "rechazado");
  public static final Set<String> VISIBILIDADES = Set.of("private", "enlace");
  public static final Set<String> DECISIONES = Set.of("aprobar", "cambios", "rechazar");
  static final int INTENTOS_ID = 5;
  private static final Pattern CONTROLES = Pattern.compile("[\\p{Cntrl}\\p{Cf}\\p{Co}\\p{Cn}]");
  private static final Pattern ESPACIOS = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final TapsCreadorGateway taps;
  private final TapsPayloadValidador validador;
  private final Clock reloj;
  private final ClientQuota guardados;
  private final ClientQuota envios;

  public TapsCreadorUseCase(TapsCreadorGateway taps, TapsPayloadValidador validador) {
    this(taps, validador, Clock.systemUTC(), new ClientQuota(MAX_GUARDADOS_POR_MINUTO, 60),
      new ClientQuota(MAX_ENVIOS_REVISION_POR_DIA, 86_400));
  }

  public TapsCreadorUseCase(TapsCreadorGateway taps, TapsPayloadValidador validador, Clock reloj, ClientQuota guardados) {
    this(taps, validador, reloj, guardados, new ClientQuota(MAX_ENVIOS_REVISION_POR_DIA, 86_400));
  }

  public TapsCreadorUseCase(TapsCreadorGateway taps, TapsPayloadValidador validador, Clock reloj, ClientQuota guardados,
                            ClientQuota envios) {
    this.taps = taps;
    this.validador = validador;
    this.reloj = reloj;
    this.guardados = guardados;
    this.envios = envios;
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
      "borrador", 0, 0, valido.json(), null, null, ahora, ahora, null);
    return taps.crear(nuevo)
      .switchIfEmpty(Mono.defer(() -> intentos > 1
        ? insertar(creador, valido, ahora, intentos - 1)
        : Mono.error(new IllegalStateException("No se pudo asignar un id al Tap"))));
  }

  /**
   * El Tap, si es de quien pregunta; 404 si no existe o es de otra persona. El SUPER_ADMIN ve además los que están en la
   * cola de revisión (para la vista previa), y solo esos.
   */
  public Mono<Tap> buscar(AuthenticatedUser quien, String id) {
    if (!TapsIds.esDeUsuario(id)) return Mono.error(new TapsCreadorNotFoundException());
    Mono<Tap> propio = taps.buscar(id, quien.userId());
    if (quien.isSuperAdmin()) propio = propio.switchIfEmpty(Mono.defer(() -> taps.buscarEnRevision(id)));
    return propio.switchIfEmpty(Mono.error(new TapsCreadorNotFoundException()));
  }

  /** Los Taps del creador; {@code estado} filtra (400 si no es un estado conocido). */
  public Flux<Tap> listar(AuthenticatedUser creador, String estado) {
    if (estado != null && !ESTADOS.contains(estado)) return Flux.error(new IllegalArgumentException("Estado desconocido"));
    return taps.listar(creador.userId(), estado);
  }

  /** La cola de revisión (solo SUPER_ADMIN; el controller ya lo exige, aquí se vuelve a comprobar). */
  public Flux<Tap> colaRevision(AuthenticatedUser quien) {
    if (!quien.isSuperAdmin()) return Flux.error(new TapsCreadorNotFoundException());
    return taps.listarEnRevision();
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

  // ── Importar el borrador de /crear (A4) ───────────────────────────────────

  /** Resultado de importar: el Tap del creador e {@code importado} = true si se creó ahora, false si ya existía. */
  public record Importado(Tap tap, boolean importado) {}

  /**
   * Trae a la cuenta del creador el personaje que diseñó en /crear antes de tener cuenta. El adjunto viene del cliente
   * (su {@code localStorage}) y se valida con {@link InstitutionRequestDraft#normalize} exactamente igual que cuando viajó
   * en la solicitud de institución: nunca se lee {@code institution_requests} (ms-education no conoce el correo del
   * usuario, así que no hay forma segura de ligar una solicitud al que pregunta, y leer por {@code borradorId} dejaría
   * leer borradores ajenos). Un adjunto inválido o un {@code borradorId} que no coincide con el del adjunto responden 404,
   * sin más detalle.
   *
   * <p>Idempotente por (dueño, borradorId): si ya existe un Tap vivo con ese origen se devuelve ese y nada se crea
   * (tampoco consume cuota). Las cuotas de {@link #crear} se aplican igual, y el contenido pasa por el validador.
   */
  public Mono<Importado> importarBorrador(AuthenticatedUser creador, String borradorId, JsonNode adjunto) {
    JsonNode canonico;
    try {
      canonico = MAPPER.readTree(InstitutionRequestDraft.normalize(InstitutionRequestDraft.asText(adjunto))
        .orElseThrow(TapsCreadorNotFoundException::new));
    } catch (TapsCreadorNotFoundException e) {
      return Mono.error(e);
    } catch (Exception e) {
      return Mono.error(new TapsCreadorNotFoundException());
    }
    String origen = canonico.get("borradorId").textValue();
    if (borradorId != null && !borradorId.equals(origen)) return Mono.error(new TapsCreadorNotFoundException());
    TapsPayloadValidador.Tap valido;
    try {
      valido = validador.validar(TapsPrimerTap.desdeAdjunto(canonico));
    } catch (IllegalArgumentException e) {
      return Mono.error(new TapsCreadorNotFoundException());
    }
    return taps.buscarPorOrigen(creador.userId(), origen)
      .map(existente -> new Importado(existente, false))
      .switchIfEmpty(Mono.defer(() -> {
        Instant ahora = reloj.instant();
        return taps.contarVivos(creador.userId())
          .flatMap(vivos -> vivos >= MAX_VIVOS
            ? Mono.error(new TapsLimiteException("Ya tienes " + MAX_VIVOS + " Taps; borra alguno para importar otro"))
            : taps.contarCreadosDesde(creador.userId(), ahora.minus(Duration.ofDays(1))))
          .flatMap(hoy -> hoy >= MAX_CREADOS_POR_DIA
            ? Mono.error(new TapsLimiteException("Hoy ya creaste " + MAX_CREADOS_POR_DIA + " Taps; sigue mañana"))
            : insertarImportado(creador, origen, valido, ahora, INTENTOS_ID));
      }));
  }

  private Mono<Importado> insertarImportado(AuthenticatedUser creador, String origen, TapsPayloadValidador.Tap valido,
                                            Instant ahora, int intentos) {
    Tap nuevo = new Tap(TapsIds.nuevo(), creador.userId(), creador.institutionId(), valido.titulo(), "private",
      "borrador", 0, 0, valido.json(), null, null, ahora, ahora, null);
    return taps.crearImportado(nuevo, origen)
      .map(t -> new Importado(t, true))
      // Vacío: o chocó el id (se reintenta) o alguien importó el mismo borrador a la vez (se devuelve ese).
      .switchIfEmpty(Mono.defer(() -> taps.buscarPorOrigen(creador.userId(), origen)
        .map(existente -> new Importado(existente, false))
        .switchIfEmpty(Mono.defer(() -> intentos > 1
          ? insertarImportado(creador, origen, valido, ahora, intentos - 1)
          : Mono.error(new IllegalStateException("No se pudo asignar un id al Tap"))))));
  }

  // ── Publicar y revisar (A3) ───────────────────────────────────────────────

  /**
   * Publica el borrador del dueño. {@code private}: directo para cualquiera. {@code enlace}: directo para docentes;
   * un CREATOR pasa a {@code en_revision} (cuota diaria). Un Tap ya en revisión responde 409; si {@code revisionEsperada}
   * no coincide, 409. El borrador se valida otra vez antes de congelarlo.
   */
  public Mono<Tap> publicar(AuthenticatedUser creador, String id, String visibilidad, Integer revisionEsperada) {
    if (!TapsIds.esDeUsuario(id)) return Mono.error(new TapsCreadorNotFoundException());
    String vis = visibilidad == null ? "enlace" : visibilidad;
    if (!VISIBILIDADES.contains(vis)) return Mono.error(new IllegalArgumentException("Visibilidad desconocida"));
    return taps.buscar(id, creador.userId())
      .switchIfEmpty(Mono.error(new TapsCreadorNotFoundException()))
      .flatMap(actual -> {
        if ("en_revision".equals(actual.estado()))
          return Mono.error(new TapsEstadoException("Este Tap ya está en revisión; espera la respuesta o retíralo"));
        if (revisionEsperada != null && revisionEsperada != actual.revision())
          return Mono.error(new TapsCreadorConflictoException());
        String canonico;
        try {
          canonico = validador.validar(actual.borrador()).json();
        } catch (IllegalArgumentException e) {
          return Mono.error(new IllegalArgumentException("El Tap necesita cambios antes de publicarse: " + e.getMessage()));
        }
        Instant ahora = reloj.instant();
        if ("enlace".equals(vis) && creador.isCreator()) {
          if (!envios.tryAcquire(String.valueOf(creador.userId())))
            return Mono.error(new TapsLimiteException("Hoy ya enviaste " + MAX_ENVIOS_REVISION_POR_DIA + " Taps a revisión; sigue mañana"));
          return taps.enviarRevision(id, creador.userId(), actual.revision(), ahora)
            .switchIfEmpty(Mono.error(new TapsCreadorConflictoException()));
        }
        return taps.publicar(id, creador.userId(), actual.revision(), vis, canonico, ahora)
          .switchIfEmpty(Mono.error(new TapsCreadorConflictoException()));
      });
  }

  /** Retira el Tap (vuelve a borrador privado); también saca uno de la cola de revisión. 409 si ya era borrador. */
  public Mono<Tap> despublicar(AuthenticatedUser creador, String id) {
    if (!TapsIds.esDeUsuario(id)) return Mono.error(new TapsCreadorNotFoundException());
    return taps.buscar(id, creador.userId())
      .switchIfEmpty(Mono.error(new TapsCreadorNotFoundException()))
      .flatMap(actual -> "borrador".equals(actual.estado())
        ? Mono.error(new TapsEstadoException("Este Tap ya es un borrador"))
        : taps.despublicar(id, creador.userId(), reloj.instant())
          .switchIfEmpty(Mono.error(new TapsCreadorConflictoException())));
  }

  /**
   * Decisión del superadmin sobre un Tap en revisión. {@code aprobar} revalida el borrador y lo congela; {@code cambios}
   * y {@code rechazar} exigen motivo (≤ 300). Un Tap que no está en la cola responde 404 (también si existe: la
   * revisión no ve borradores ajenos). Si el borrador cambió desde lo que vio el revisor ({@code revisionEsperada}), 409.
   */
  public Mono<Tap> decidir(AuthenticatedUser revisor, String id, String decision, String motivo, Integer revisionEsperada) {
    if (!revisor.isSuperAdmin() || !TapsIds.esDeUsuario(id)) return Mono.error(new TapsCreadorNotFoundException());
    if (decision == null || !DECISIONES.contains(decision)) return Mono.error(new IllegalArgumentException("Decisión desconocida"));
    String nota;
    try {
      nota = limpiarMotivo(motivo);
    } catch (IllegalArgumentException e) {
      return Mono.error(e);
    }
    if (!"aprobar".equals(decision) && nota == null)
      return Mono.error(new IllegalArgumentException("Explica al creador qué debe cambiar"));
    return taps.buscarEnRevision(id)
      .switchIfEmpty(Mono.error(new TapsCreadorNotFoundException()))
      .flatMap(actual -> {
        if (revisionEsperada != null && revisionEsperada != actual.revision())
          return Mono.error(new TapsCreadorConflictoException());
        Instant ahora = reloj.instant();
        if ("aprobar".equals(decision)) {
          String canonico;
          try {
            canonico = validador.validar(actual.borrador()).json();
          } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("El Tap no pasa la validación automática: " + e.getMessage()));
          }
          return taps.aprobar(id, actual.revision(), canonico, revisor.userId(), ahora)
            .switchIfEmpty(Mono.error(new TapsCreadorConflictoException()));
        }
        return taps.rechazar(id, actual.revision(), nota, revisor.userId(), ahora)
          .switchIfEmpty(Mono.error(new TapsCreadorConflictoException()));
      });
  }

  /** El motivo recortado y sin controles; null si viene vacío; 400 si es demasiado largo. */
  static String limpiarMotivo(String motivo) {
    if (motivo == null) return null;
    String s = ESPACIOS.matcher(Normalizer.normalize(motivo, Normalizer.Form.NFC)).replaceAll(" ").trim();
    if (s.isEmpty()) return null;
    if (s.codePointCount(0, s.length()) > MOTIVO_MAX) throw new IllegalArgumentException("El motivo supera los " + MOTIVO_MAX + " caracteres");
    if (CONTROLES.matcher(s).find()) throw new IllegalArgumentException("El motivo tiene caracteres no admitidos");
    return s;
  }
}
