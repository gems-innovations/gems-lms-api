package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsEstadisticasUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Estadísticas anónimas por Tap, públicas (/api/v1/public/**): la web envía un evento por vista, respuesta, final y
 * compartido, y cualquiera puede leer las cifras agregadas (sirven para ordenar y destacar Taps). No se guarda la IP.
 */
@RestController
public class TapsEstadisticasController {
  private final TapsEstadisticasUseCase estadisticas;

  public TapsEstadisticasController(TapsEstadisticasUseCase estadisticas) {
    this.estadisticas = estadisticas;
  }

  @PostMapping("/api/v1/public/taps/eventos")
  public Mono<ResponseEntity<Void>> evento(@RequestBody TapsEstadisticasUseCase.Evento evento) {
    return Mono.defer(() -> estadisticas.registrar(evento)).thenReturn(ResponseEntity.noContent().cacheControl(CacheControl.noStore()).<Void>build());
  }

  @GetMapping("/api/v1/public/taps/estadisticas")
  public Mono<ResponseEntity<TapsEstadisticasUseCase.Resumen>> leer(@RequestParam(required = false) Integer dias) {
    return Mono.defer(() -> estadisticas.leer(dias))
      .map(r -> ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic()).body(r));
  }
}