package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsComentariosUseCase;
import com.gems.education.application.TapsCuentas;
import com.gems.education.application.TapsSocialesUseCase;
import com.gems.shared.security.CurrentUser;
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
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Cifras sociales de los Taps. Públicas (/api/v1/public/**): solo cifras, caché pública de 60 s. Con sesión
 * (/api/v1/taps/**, cualquier rol con cuenta registrada; un invitado recibe 403 TAPS_SOCIAL_REQUIERE_CUENTA): el me
 * gusta propio y los comentarios, nunca en caché. La identidad es la del token.
 */
@RestController
public class TapsSocialesController {
  private final TapsSocialesUseCase sociales;
  private final TapsComentariosUseCase comentarios;
  private final TapsCuentas cuentas;

  public TapsSocialesController(TapsSocialesUseCase sociales, TapsComentariosUseCase comentarios, TapsCuentas cuentas) {
    this.sociales = sociales;
    this.comentarios = comentarios;
    this.cuentas = cuentas;
  }

  public record MeGustaRequest(Boolean meGusta) {}

  public record ComentarioRequest(String texto) {}

  public record EstadoRequest(Boolean oculto) {}

  @GetMapping("/api/v1/public/taps/sociales")
  public Mono<ResponseEntity<TapsSocialesUseCase.Sociales>> leer(@RequestParam(required = false) String ids) {
    return sociales.leer(ids)
      .map(s -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic()).body(s));
  }

  @GetMapping("/api/v1/taps/sociales/yo")
  public Mono<ResponseEntity<TapsSocialesUseCase.Mios>> mios(@RequestParam(required = false) String ids) {
    return cuenta().flatMap(u -> sociales.mios(u.userId(), ids)).map(TapsSocialesController::privado);
  }

  @PutMapping("/api/v1/taps/{tapId}/me-gusta")
  public Mono<ResponseEntity<TapsSocialesUseCase.Estado>> meGusta(@PathVariable String tapId, @RequestBody MeGustaRequest body) {
    return cuenta().flatMap(u -> sociales.cambiar(u.userId(), tapId, body == null ? null : body.meGusta()))
      .map(TapsSocialesController::privado);
  }

  @GetMapping("/api/v1/taps/{tapId}/comentarios")
  public Mono<ResponseEntity<TapsComentariosUseCase.Pagina>> comentarios(@PathVariable String tapId,
                                                                        @RequestParam(required = false) Long antes) {
    return cuenta().flatMap(u -> comentarios.leer(u, tapId, antes)).map(TapsSocialesController::privado);
  }

  @PostMapping("/api/v1/taps/{tapId}/comentarios")
  public Mono<ResponseEntity<TapsComentariosUseCase.Vista>> comentar(@PathVariable String tapId,
                                                                    @RequestBody ComentarioRequest body) {
    return cuenta().flatMap(u -> comentarios.comentar(u, tapId, body == null ? null : body.texto()))
      .map(c -> ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(c));
  }

  @DeleteMapping("/api/v1/taps/comentarios/{id}")
  public Mono<ResponseEntity<Void>> borrar(@PathVariable long id) {
    return cuenta().flatMap(u -> comentarios.borrar(u, id)).thenReturn(ResponseEntity.noContent().<Void>build());
  }

  @PostMapping("/api/v1/taps/comentarios/{id}/reportar")
  public Mono<ResponseEntity<Void>> reportar(@PathVariable long id) {
    return cuenta().flatMap(u -> comentarios.reportar(u, id)).thenReturn(ResponseEntity.noContent().<Void>build());
  }

  @PutMapping("/api/v1/taps/comentarios/{id}/estado")
  public Mono<ResponseEntity<Void>> estado(@PathVariable long id, @RequestBody EstadoRequest body) {
    return cuenta().flatMap(u -> comentarios.cambiarEstado(u, id, body == null ? null : body.oculto()))
      .thenReturn(ResponseEntity.noContent().<Void>build());
  }

  /** El usuario del token si su cuenta no es de invitado. */
  private Mono<com.gems.shared.security.AuthenticatedUser> cuenta() {
    return CurrentUser.get().flatMap(cuentas::registrada);
  }

  private static <T> ResponseEntity<T> privado(T body) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(body);
  }
}
