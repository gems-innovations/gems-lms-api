package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsSocialesUseCase;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.shared.security.SecurityExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsSocialesControllerTest {
  private static final String YO = "11111111-2222-3333-4444-555555555555";
  private final TapsSocialesUseCase caso = mock(TapsSocialesUseCase.class);
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToController(new TapsSocialesController(caso))
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice()).build();
  }

  @Test
  void elMeGustaRespondeElEstadoSinCache() {
    when(caso.cambiar(eq("mc-gancho"), any())).thenReturn(Mono.just(new TapsSocialesUseCase.Estado("mc-gancho", true, 4)));
    client.post().uri("/api/v1/public/taps/mc-gancho/me-gusta").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"jugador\":\"" + YO + "\",\"meGusta\":true}").exchange()
      .expectStatus().isOk()
      .expectHeader().cacheControl(CacheControl.noStore())
      .expectBody().json("{\"tapId\":\"mc-gancho\",\"meGusta\":true,\"total\":4}");
    verify(caso).cambiar("mc-gancho", new TapsSocialesUseCase.Cambio(YO, true));
  }

  @Test
  void unTapDesconocidoResponde400YElLimite429() {
    when(caso.cambiar(any(), any())).thenReturn(Mono.error(new IllegalArgumentException("Tap desconocido")));
    client.post().uri("/api/v1/public/taps/no-existe/me-gusta").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"jugador\":\"" + YO + "\",\"meGusta\":true}").exchange().expectStatus().isBadRequest();
    when(caso.cambiar(any(), any())).thenReturn(Mono.error(new TapsLimiteException("espera")));
    client.post().uri("/api/v1/public/taps/mc-gancho/me-gusta").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"jugador\":\"" + YO + "\",\"meGusta\":true}").exchange().expectStatus().isEqualTo(429);
  }

  @Test
  void unCuerpoMalFormadoResponde400() {
    client.post().uri("/api/v1/public/taps/mc-gancho/me-gusta").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"jugador\":\"" + YO + "\",\"meGusta\":\"quizas\"}").exchange().expectStatus().isBadRequest();
  }

  @Test
  void sinJugadorLaLecturaEsPublicaYSeCachea60s() {
    when(caso.leer("mc-gancho,fleming", null)).thenReturn(Mono.just(new TapsSocialesUseCase.Sociales(
      List.of(new TapsSocialesUseCase.Social("mc-gancho", 12, 3, null)))));
    client.get().uri("/api/v1/public/taps/sociales?ids=mc-gancho,fleming").exchange().expectStatus().isOk()
      .expectHeader().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
      .expectBody().json("{\"taps\":[{\"tapId\":\"mc-gancho\",\"vistas\":12,\"meGusta\":3}]}")
      .jsonPath("$.taps[0].meGustaYo").doesNotExist();
  }

  @Test
  void conJugadorLaLecturaNoSeGuardaEnCache() {
    when(caso.leer("mc-gancho", YO)).thenReturn(Mono.just(new TapsSocialesUseCase.Sociales(
      List.of(new TapsSocialesUseCase.Social("mc-gancho", 12, 3, true)))));
    client.get().uri("/api/v1/public/taps/sociales?ids=mc-gancho&jugador=" + YO).exchange().expectStatus().isOk()
      .expectHeader().cacheControl(CacheControl.noStore().cachePrivate())
      .expectBody().jsonPath("$.taps[0].meGustaYo").isEqualTo(true);
  }

  @Test
  void unaLecturaInvalidaResponde400() {
    when(caso.leer(any(), any())).thenReturn(Mono.error(new IllegalArgumentException("Máximo 60 ids")));
    client.get().uri("/api/v1/public/taps/sociales?ids=a").exchange().expectStatus().isBadRequest();
  }
}
