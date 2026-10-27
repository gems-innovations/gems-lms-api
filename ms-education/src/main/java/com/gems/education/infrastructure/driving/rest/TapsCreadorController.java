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
 * Borradores de Taps del creador autenticado (PR-10 A2). Solo quien crea Taps (CREATOR, INSTRUCTOR, ADMIN, SUPER_ADMIN;
 * un STUDENT recibe 403). Cada Tap lo ve y lo edita únicamente su dueño: uno ajeno responde 404, nunca 403. El contenido
 * entra como JSON crudo y lo canoniza TapsPayloadValidador; nunca se devuelve como HTML. Las respuestas llevan
 * {@code ETag: "<revision>"} y PUT admite {@code If-Match} para el autoguardado (409 si cambió).
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

  /** Lo que ve el creador de su Tap. {@code borrador} es el contenido canónico como objeto. */
  public record TapResponse(String id, String titulo, String visibilidad, String estado, int version, int revision,
                            JsonNode borrador, String motivo, Instant creado, Instant actualizado, Instant publicadoEn) {}

  @GetMapping
  public Mono<ResponseEntity<List<TapResponse>>> mios(@RequestParam(required = false) String estado) {
    return creador()
      .flatMapMany(u -> taps.listar(u, estado))
      .map(this::toResponse)
      .collectList()
      .map(lista -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(lista));
  }

  @PostMapping
  public Mono<ResponseEntity<TapResponse>> crear(@RequestBody JsonNode contenido) {
    return creador()
      .flatMap(u -> taps.crear(u, contenido))
      .map(t -> conEtag(ResponseEntity.status(HttpStatus.CREATED), t));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<TapResponse>> uno(@PathVariable String id) {
    return creador()
      .flatMap(u -> taps.buscar(u, id))
      .map(t -> conEtag(ResponseEntity.ok(), t));
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
      .map(t -> conEtag(ResponseEntity.ok(), t));
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> borrar(@PathVariable String id) {
    return creador()
      .flatMap(u -> taps.borrar(u, id))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  private static Mono<AuthenticatedUser> creador() {
    return CurrentUser.require(AuthenticatedUser::createsTaps, "Only Taps creators and institution staff can do this");
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

  private ResponseEntity<TapResponse> conEtag(ResponseEntity.BodyBuilder builder, Tap t) {
    return builder.eTag("\"" + t.revision() + "\"").cacheControl(CacheControl.noStore()).body(toResponse(t));
  }

  private TapResponse toResponse(Tap t) {
    JsonNode borrador;
    try {
      borrador = mapper.readTree(t.borrador());
    } catch (Exception e) {
      throw new IllegalStateException("Borrador ilegible del Tap " + t.id());
    }
    return new TapResponse(t.id(), t.titulo(), t.visibilidad(), t.estado(), t.version(), t.revision(), borrador,
      t.motivo(), t.creado(), t.actualizado(), t.publicadoEn());
  }
}
