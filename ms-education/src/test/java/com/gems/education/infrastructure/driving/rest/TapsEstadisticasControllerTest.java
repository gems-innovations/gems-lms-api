package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsEstadisticasUseCase;
import com.gems.shared.security.SecurityExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsEstadisticasControllerTest {
  private final TapsEstadisticasUseCase caso = mock(TapsEstadisticasUseCase.class);
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToController(new TapsEstadisticasController(caso))
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice()).build();
  }

  @Test
  void elEventoSeRegistraYResponde204() {
    when(caso.registrar(any())).thenReturn(Mono.empty());
    client.post().uri("/api/v1/public/taps/eventos").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"tapId\":\"mc-gancho\",\"evento\":\"respuesta\",\"pregunta\":1,\"acierto\":true}").exchange()
      .expectStatus().isNoContent();
    verify(caso).registrar(new TapsEstadisticasUseCase.Evento("mc-gancho", "respuesta", 1, true));
  }

  @Test
  void unEventoInvalidoResponde400() {
    when(caso.registrar(any())).thenThrow(new IllegalArgumentException("Tap desconocido"));
    client.post().uri("/api/v1/public/taps/eventos").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"tapId\":\"x\",\"evento\":\"vista\"}").exchange().expectStatus().isBadRequest();
  }

  @Test
  void lasEstadisticasSeLeenYSeCachean() {
    when(caso.leer(7)).thenReturn(Mono.just(new TapsEstadisticasUseCase.Resumen("2026-10-18", "2026-10-24", 7, "mc-gancho",
      List.of(new TapsEstadisticasUseCase.TapEstadistica("mc-gancho", "minecraft", 10, 8, 5, 1, 30, 20,
        List.of(new TapsEstadisticasUseCase.Pregunta(0, 8, 6, 1)))))));
    client.get().uri("/api/v1/public/taps/estadisticas?dias=7").exchange().expectStatus().isOk()
      .expectHeader().cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofSeconds(60)).cachePublic())
      .expectBody().jsonPath("$.masVisto").isEqualTo("mc-gancho")
      .jsonPath("$.taps[0].vistas").isEqualTo(10)
      .jsonPath("$.taps[0].preguntas[0].abandonos").isEqualTo(1);
  }
}