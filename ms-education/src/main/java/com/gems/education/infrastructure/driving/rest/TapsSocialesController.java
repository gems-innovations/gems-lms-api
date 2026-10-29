package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsSocialesUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * «Me gusta» anónimos y cifras sociales de los Taps, públicos (/api/v1/public/**). Sin jugador la lectura se cachea
 * 60 s en público; con jugador (dice qué le gustó a ese dispositivo) no se guarda en ninguna caché.
 */
@RestController
public class TapsSocialesController {
  private final TapsSocialesUseCase sociales;

  public TapsSocialesController(TapsSocialesUseCase sociales) {
    this.sociales = sociales;
  }

  @PostMapping("/api/v1/public/taps/{tapId}/me-gusta")
  public Mono<ResponseEntity<TapsSocialesUseCase.Estado>> meGusta(@PathVariable String tapId,
                                                                  @RequestBody TapsSocialesUseCase.Cambio cambio) {
    return sociales.cambiar(tapId, cambio).map(e -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(e));
  }

  @GetMapping("/api/v1/public/taps/sociales")
  public Mono<ResponseEntity<TapsSocialesUseCase.Sociales>> leer(@RequestParam(required = false) String ids,
                                                                 @RequestParam(required = false) String jugador) {
    CacheControl cache = jugador == null || jugador.isBlank()
      ? CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic()
      : CacheControl.noStore().cachePrivate();
    return sociales.leer(ids, jugador).map(s -> ResponseEntity.ok().cacheControl(cache).body(s));
  }
}
