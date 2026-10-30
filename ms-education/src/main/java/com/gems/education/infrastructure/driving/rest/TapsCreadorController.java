package com.gems.education.infrastructure.driving.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gems.education.application.TapsCreadorUseCase;
import com.gems.education.application.gateway.TapsCreadorGateway.Tap;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.CurrentUser;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Taps del creador autenticado (PR-10 A2 y A3). Solo quien crea Taps (CREATOR, INSTRUCTOR, ADMIN, SUPER_ADMIN; un STUDENT
 * recibe 403). Cada Tap lo ve y lo edita únicamente su dueño: uno ajeno responde 404, nunca 403. El contenido entra como
 * JSON crudo y lo canoniza TapsPayloadValidador; nunca se devuelve como HTML. Las respuestas llevan
 * {@code ETag: "<revision>"}; PUT, publicar y la decisión admiten {@code If-Match} (409 si cambió).
 *
 * <p>Revisión: el SUPER_ADMIN lista la cola con {@code GET /api/v1/taps?revision=1}, ve cada Tap en cola con
 * {@code GET /api/v1/taps/{id}} y decide con {@code POST /api/v1/taps/{id}/revision}. Nada más sale de la propiedad
 * estricta.
 */
@RestController
@RequestMapping("/api/v1/taps")
public class TapsCreadorController {
  private final TapsCreadorUseCase taps;
  private final ObjectMapper mapper;

  public TapsCreadorController(TapsCreadorUseCase taps, ObjectMapper mapper) {
    this.taps = taps;
    this.mapper = mapper;
  }

  /**
   * Lo que ve el creador de su Tap. {@code borrador} es el contenido canónico que edita; {@code publicado} la copia en el
   * aire (null si nunca se publicó). En la cola de revisión del superadmin se añade el dueño ({@code ownerId},
   * {@code institutionId}); para el dueño van en null.
   */
  public record TapResponse(String id, String titulo, String visibilidad, String estado, int version, int revision,
                            JsonNode borrador, JsonNode publicado, String motivo, Instant creado, Instant actualizado,
                            Instant publicadoEn, Long ownerId, String institutionId) {}

  public record PublicarRequest(String visibilidad) {}

  public record RevisionRequest(String decision, String motivo) {}

  /** {@code adjunto}: el borrador de /crear tal como viajó en la solicitud ({@code {v,borradorId,b,l,i,w}}), objeto o texto JSON. */
  public record ImportarRequest(String borradorId, JsonNode adjunto) {}

  /** {@code importado}: true si el Tap se creó ahora; false si ese borrador ya se había importado (se devuelve el mismo). */
  public record ImportarResponse(String id, boolean importado, TapResponse tap) {}

  @GetMapping
  public Mono<ResponseEntity<List<TapResponse>>> mios(@RequestParam(required = false) String estado,
                                                     @RequestParam(required = false) String revision) {
    if (revision != null) {
      return superadmin()
        .flatMapMany(taps::colaRevision)
        .map(t -> toResponse(t, true))
        .collectList()
        .map(lista -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(lista));
    }
    return creador()
      .flatMapMany(u -> taps.listar(u, estado))
      .map(t -> toResponse(t, false))
      .collectList()
      .map(lista -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(lista));
  }

  @PostMapping
  public Mono<ResponseEntity<TapResponse>> crear(@RequestBody JsonNode contenido) {
    return creador()
      .flatMap(u -> taps.crear(u, contenido))
      .map(t -> conEtag(ResponseEntity.status(HttpStatus.CREATED), t, false));
  }

  /**
   * Importa a la cuenta el personaje diseñado en /crear (PR-10 §6). El cuerpo trae el adjunto; el servidor lo valida como
   * cuando viajó en la solicitud y crea «Mi primer Tap» privado. 201 si se creó, 200 si ya existía (idempotente por
   * borrador y dueño), 404 si el adjunto no es válido. Nunca lee solicitudes de institución.
   */
  @PostMapping("/importar-borrador")
  public Mono<ResponseEntity<ImportarResponse>> importar(@RequestBody ImportarRequest cuerpo) {
    return creador()
      .flatMap(u -> taps.importarBorrador(u, cuerpo.borradorId(), cuerpo.adjunto()))
      .map(r -> ResponseEntity.status(r.importado() ? HttpStatus.CREATED : HttpStatus.OK)
        .eTag("\"" + r.tap().revision() + "\"").cacheControl(CacheControl.noStore())
        .body(new ImportarResponse(r.tap().id(), r.importado(), toResponse(r.tap(), false))));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<TapResponse>> uno(@PathVariable String id) {
    return creador()
      .flatMap(u -> taps.buscar(u, id).map(t -> conEtag(ResponseEntity.ok(), t, u.isSuperAdmin() && !u.isUser(t.ownerId()))));
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<TapResponse>> guardar(@PathVariable String id,
                                                   @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                                   @RequestBody JsonNode contenido) {
    Integer revision;
    try {
      revision = revisionDe(ifMatch);
    } catch (IllegalArgumentException e) {
      return Mono.error(e);
    }
    return creador()
      .flatMap(u -> taps.actualizar(u, id, revision, contenido))
      .map(t -> conEtag(ResponseEntity.ok(), t, false));
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> borrar(@PathVariable String id) {
    return creador()
      .flatMap(u -> taps.borrar(u, id))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  /** {@code {visibilidad:'enlace'|'private'}} (enlace por defecto). Docentes: publicado directo; CREATOR con enlace: a revisión. */
  @PostMapping("/{id}/publicar")
  public Mono<ResponseEntity<TapResponse>> publicar(@PathVariable String id,
                                                    @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                                    @RequestBody(required = false) PublicarRequest cuerpo) {
    Integer revision;
    try {
      revision = revisionDe(ifMatch);
    } catch (IllegalArgumentException e) {
      return Mono.error(e);
    }
    String visibilidad = cuerpo == null ? null : cuerpo.visibilidad();
    return creador()
      .flatMap(u -> taps.publicar(u, id, visibilidad, revision))
      .map(t -> conEtag(ResponseEntity.ok(), t, false));
  }

  @PostMapping("/{id}/despublicar")
  public Mono<ResponseEntity<TapResponse>> despublicar(@PathVariable String id) {
    return creador()
      .flatMap(u -> taps.despublicar(u, id))
      .map(t -> conEtag(ResponseEntity.ok(), t, false));
  }

  /** Solo SUPER_ADMIN: {@code {decision:'aprobar'|'cambios'|'rechazar', motivo}}; If-Match con la revisión que revisó. */
  @PostMapping("/{id}/revision")
  public Mono<ResponseEntity<TapResponse>> revision(@PathVariable String id,
                                                    @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
                                                    @RequestBody RevisionRequest cuerpo) {
    Integer revision;
    try {
      revision = revisionDe(ifMatch);
    } catch (IllegalArgumentException e) {
      return Mono.error(e);
    }
    return superadmin()
      .flatMap(u -> taps.decidir(u, id, cuerpo.decision(), cuerpo.motivo(), revision))
      .map(t -> conEtag(ResponseEntity.ok(), t, true));
  }

  private static Mono<AuthenticatedUser> creador() {
    return CurrentUser.require(AuthenticatedUser::createsTaps, "Only Taps creators and institution staff can do this");
  }

  private static Mono<AuthenticatedUser> superadmin() {
    return CurrentUser.require(AuthenticatedUser::isSuperAdmin, "Only the super admin reviews Taps");
  }

  /** La revisión de un If-Match {@code "N"} (comillas opcionales); null si no viene; 400 si no es un número. */
  static Integer revisionDe(String ifMatch) {
    if (ifMatch == null || ifMatch.isBlank()) return null;
    String s = ifMatch.trim();
    if (s.startsWith("W/")) s = s.substring(2);
    if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
    if (!s.matches("\\d{1,9}")) throw new IllegalArgumentException("If-Match no es una revisión válida");
    return Integer.valueOf(s);
  }

  private ResponseEntity<TapResponse> conEtag(ResponseEntity.BodyBuilder builder, Tap t, boolean conDueno) {
    return builder.eTag("\"" + t.revision() + "\"").cacheControl(CacheControl.noStore()).body(toResponse(t, conDueno));
  }

  private TapResponse toResponse(Tap t, boolean conDueno) {
    return new TapResponse(t.id(), t.titulo(), t.visibilidad(), t.estado(), t.version(), t.revision(),
      arbol(t.borrador(), t.id()), t.publicado() == null ? null : arbol(t.publicado(), t.id()), t.motivo(), t.creado(),
      t.actualizado(), t.publicadoEn(), conDueno ? t.ownerId() : null, conDueno ? t.institutionId() : null);
  }

  private JsonNode arbol(String json, String id) {
    try {
      return mapper.readTree(json);
    } catch (Exception e) {
      throw new IllegalStateException("Contenido ilegible del Tap " + id);
    }
  }
}
