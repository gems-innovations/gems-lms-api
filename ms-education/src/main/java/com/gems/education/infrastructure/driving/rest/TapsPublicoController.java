package com.gems.education.infrastructure.driving.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gems.education.application.TapsIds;
import com.gems.education.application.exceptions.TapsCreadorNotFoundException;
import com.gems.education.application.gateway.TapsCreadorGateway;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Un Tap de creador publicado con enlace, para jugarlo sin cuenta (PR-10 A3). Solo la copia congelada y revisada: nunca
 * el borrador, nunca el dueño (ni id ni correo). Lo que no está publicado con enlace (borrador, en revisión sin versión
 * aprobada, rechazado, retirado, borrado o inexistente) responde 404, siempre igual: no se revela que exista.
 */
@RestController
public class TapsPublicoController {
  private final TapsCreadorGateway taps;
  private final ObjectMapper mapper;

  public TapsPublicoController(TapsCreadorGateway taps, ObjectMapper mapper) {
    this.taps = taps;
    this.mapper = mapper;
  }

  @GetMapping("/api/v1/public/taps/u/{id}")
  public Mono<ResponseEntity<JsonNode>> publicado(@PathVariable String id) {
    if (!TapsIds.esDeUsuario(id)) return Mono.error(new TapsCreadorNotFoundException());
    return taps.buscarPublicado(id)
      .switchIfEmpty(Mono.error(new TapsCreadorNotFoundException()))
      .map(p -> ResponseEntity.ok()
        .eTag("\"" + p.version() + "\"")
        .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
        .body(cuerpo(p)));
  }

  /** La copia publicada como objeto, con su id y {@code soloEnlace:true}; sin ninguna otra columna de la fila. */
  private JsonNode cuerpo(TapsCreadorGateway.Publicado p) {
    ObjectNode tap;
    try {
      tap = (ObjectNode) mapper.readTree(p.publicado());
    } catch (Exception e) {
      throw new IllegalStateException("Copia publicada ilegible del Tap " + p.id());
    }
    tap.put("id", p.id());
    tap.put("version", p.version());
    tap.put("soloEnlace", true);
    return tap;
  }
}
