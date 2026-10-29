package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsSocialUseCase;
import com.gems.shared.security.JwtReactiveAuthenticationManager;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Likes y comentarios de Taps, bajo /api/v1/public/** (público en el gateway y aquí). La sesión es opcional para
 * leer y dar like (sin sesión cuenta el dispositivo, {@code jugadorId}); comentar, borrar y reportar responden 401
 * sin un Bearer válido. Reglas en {@link TapsSocialUseCase}.
 */
@RestController
public class TapsSocialController {
  private static final String BASE = "/api/v1/public/taps/{tapId}";

  private final TapsSocialUseCase social;
  private final JwtReactiveAuthenticationManager jwt;

  public TapsSocialController(TapsSocialUseCase social, JwtReactiveAuthenticationManager jwt) {
    this.social = social;
    this.jwt = jwt;
  }

  @GetMapping(BASE + "/social")
  public Mono<ResponseEntity<TapsSocialUseCase.Social>> leer(@PathVariable String tapId,
                                                             @RequestParam(required = false) String jugadorId,
                                                             ServerWebExchange exchange) {
    return TapsController.usuario(exchange, jwt).flatMap(u -> social.social(tapId, u.orElse(null), jugadorId)).map(TapsSocialController::ok);
  }

  @PutMapping(BASE + "/like")
  public Mono<ResponseEntity<TapsSocialUseCase.Social>> darLike(@PathVariable String tapId,
                                                                @RequestParam(required = false) String jugadorId,
                                                                ServerWebExchange exchange) {
    return TapsController.usuario(exchange, jwt).flatMap(u -> social.like(tapId, true, u.orElse(null), jugadorId)).map(TapsSocialController::ok);
  }

  @DeleteMapping(BASE + "/like")
  public Mono<ResponseEntity<TapsSocialUseCase.Social>> quitarLike(@PathVariable String tapId,
                                                                   @RequestParam(required = false) String jugadorId,
                                                                   ServerWebExchange exchange) {
    return TapsController.usuario(exchange, jwt).flatMap(u -> social.like(tapId, false, u.orElse(null), jugadorId)).map(TapsSocialController::ok);
  }

  @GetMapping(BASE + "/comentarios")
  public Mono<ResponseEntity<TapsSocialUseCase.Pagina>> comentarios(@PathVariable String tapId,
                                                                    @RequestParam(required = false) String cursor,
                                                                    @RequestParam(required = false) Integer limite,
                                                                    ServerWebExchange exchange) {
    return TapsController.usuario(exchange, jwt).flatMap(u -> social.comentarios(tapId, cursor, limite, u.orElse(null))).map(TapsSocialController::ok);
  }

  @PostMapping(BASE + "/comentarios")
  public Mono<ResponseEntity<TapsSocialUseCase.Comentario>> comentar(@PathVariable String tapId,
                                                                     @RequestBody TapsSocialUseCase.Nuevo nuevo,
                                                                     ServerWebExchange exchange) {
    return TapsController.usuario(exchange, jwt).flatMap(u -> u.isEmpty() ? Mono.just(sinSesion())
      : social.comentar(tapId, nuevo, u.get()).map(c -> ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(c)));
  }

  @DeleteMapping(BASE + "/comentarios/{id}")
  public Mono<ResponseEntity<Void>> borrar(@PathVariable String tapId, @PathVariable long id, ServerWebExchange exchange) {
    return TapsController.usuario(exchange, jwt).flatMap(u -> u.isEmpty() ? Mono.just(TapsSocialController.<Void>sinSesion())
      : social.borrar(tapId, id, u.get()).map(ok -> ok ? ResponseEntity.noContent().<Void>build() : ResponseEntity.notFound().<Void>build()));
  }

  @PostMapping(BASE + "/comentarios/{id}/reportar")
  public Mono<ResponseEntity<Void>> reportar(@PathVariable String tapId, @PathVariable long id, ServerWebExchange exchange) {
    return TapsController.usuario(exchange, jwt).flatMap(u -> u.isEmpty() ? Mono.just(TapsSocialController.<Void>sinSesion())
      : social.reportar(tapId, id, u.get()).thenReturn(ResponseEntity.noContent().<Void>build()));
  }

  private static <T> ResponseEntity<T> ok(T body) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
  }

  private static <T> ResponseEntity<T> sinSesion() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
