package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsRankingUseCase;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsGateway;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.JwtReactiveAuthenticationManager;
import com.gems.shared.security.SecurityExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsControllerTest {
  private static final String ID = "3f1c2b7a-9d4e-4c1a-8b2f-0a1b2c3d4e5f";
  private static final String CUERPO = "{\"jugadorId\":\"" + ID + "\",\"tapId\":\"mc-gancho\",\"tema\":\"minecraft\","
    + "\"aciertos\":4,\"total\":5,\"duracionMs\":41000}";

  private final TapsRankingUseCase taps = mock(TapsRankingUseCase.class);
  private final JwtReactiveAuthenticationManager jwt = mock(JwtReactiveAuthenticationManager.class);
  private WebTestClient client;

  private static final TapsRankingUseCase.Resultado RESULTADO = new TapsRankingUseCase.Resultado("Creeper Veloz 482", 40,
    new TapsRankingUseCase.Semana(7, 120, 340), new TapsRankingUseCase.Hoy(72, 45));

  @BeforeEach
  void setUp() {
    // Sin filtro de seguridad: la ruta es pública, igual que en el gateway y en SecurityConfig.
    client = WebTestClient.bindToController(new TapsController(taps, jwt))
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice())
      .build();
    lenient().when(taps.registrar(any(), any())).thenReturn(Mono.just(RESULTADO));
  }

  private WebTestClient.ResponseSpec enviar(String cuerpo, String bearer) {
    WebTestClient.RequestBodySpec req = client.post().uri("/api/v1/public/taps/partidas").contentType(MediaType.APPLICATION_JSON);
    if (bearer != null) req = req.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
    return req.bodyValue(cuerpo).exchange();
  }

  @Test
  void sinSesionLaPartidaEsAnonimaYRespondeElContrato() {
    enviar(CUERPO, null).expectStatus().isOk()
      .expectBody()
      .jsonPath("$.alias").isEqualTo("Creeper Veloz 482")
      .jsonPath("$.xpGanado").isEqualTo(40)
      .jsonPath("$.semana.puesto").isEqualTo(7)
      .jsonPath("$.semana.jugadores").isEqualTo(120)
      .jsonPath("$.semana.xp").isEqualTo(340)
      .jsonPath("$.hoy.percentil").isEqualTo(72)
      .jsonPath("$.hoy.jugadores").isEqualTo(45);
    verify(taps).registrar(new TapsRankingUseCase.Partida(ID, "mc-gancho", "minecraft", 4, 5, 41_000L), null);
  }

  @Test
  void conBearerValidoLaPartidaSeAsociaAlUsuario() {
    when(jwt.authenticate(any())).thenReturn(Mono.just(new UsernamePasswordAuthenticationToken(
      new AuthenticatedUser(9L, "STUDENT", "gems-abierto"), "tok", List.of())));

    enviar(CUERPO, "tok").expectStatus().isOk();
    verify(taps).registrar(any(), eq(9L));
  }

  @Test
  void conBearerInvalidoSigueComoAnonimo() {
    when(jwt.authenticate(any())).thenReturn(Mono.error(new BadCredentialsException("Invalid or expired JWT")));

    enviar(CUERPO, "caducado").expectStatus().isOk();
    verify(taps).registrar(any(), isNull());
  }

  @Test
  void datosInvalidosResponden400() {
    // Con el caso de uso real (y la base simulada) para que la validación sea la de verdad.
    TapsGateway gateway = mock(TapsGateway.class);
    WebTestClient real = WebTestClient.bindToController(new TapsController(
        new TapsRankingUseCase(gateway, com.gems.education.application.TapsBanco.delClasspath(), Clock.fixed(Instant.parse("2026-10-21T20:00:00Z"), ZoneOffset.UTC), new Random(1)), jwt))
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice()).build();

    real.post().uri("/api/v1/public/taps/partidas").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(CUERPO.replace("\"aciertos\":4", "\"aciertos\":6")).exchange()
      .expectStatus().isBadRequest();
    real.post().uri("/api/v1/public/taps/partidas").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(CUERPO.replace("minecraft", "historia")).exchange()
      .expectStatus().isBadRequest();
    real.post().uri("/api/v1/public/taps/partidas").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{no es json").exchange()
      .expectStatus().isBadRequest();
    real.post().uri("/api/v1/public/taps/partidas").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(CUERPO.replace("mc-gancho", "mc-inventado")).exchange()
      .expectStatus().isBadRequest();
    real.post().uri("/api/v1/public/taps/partidas").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(CUERPO.replace("\"total\":5", "\"total\":6")).exchange()
      .expectStatus().isBadRequest();
    real.get().uri("/api/v1/public/taps/ranking?periodo=mes").exchange().expectStatus().isBadRequest();
    verifyNoInteractions(gateway);
  }

  @Test
  void demasiadasPartidasResponden429() {
    when(taps.registrar(any(), any())).thenReturn(Mono.error(new TapsLimiteException("Espera unos segundos entre partidas")));

    enviar(CUERPO, null).expectStatus().isEqualTo(429)
      .expectBody().jsonPath("$.code").isEqualTo(TapsLimiteException.CODE);
  }

  @Test
  void rankingRespondeElContratoConYoNuloSinJugador() {
    when(taps.ranking("semana", null, null, null, null)).thenReturn(Mono.just(new TapsRankingUseCase.Ranking("semana",
      "2026-10-19T05:00:00Z", "2026-10-26T05:00:00Z", 120,
      List.of(new TapsRankingUseCase.Entrada(1, "Ghast Sabio 113", 900, false)), null)));

    client.get().uri("/api/v1/public/taps/ranking?periodo=semana").exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.periodo").isEqualTo("semana")
      .jsonPath("$.desde").isEqualTo("2026-10-19T05:00:00Z")
      .jsonPath("$.hasta").isEqualTo("2026-10-26T05:00:00Z")
      .jsonPath("$.jugadores").isEqualTo(120)
      .jsonPath("$.top[0].puesto").isEqualTo(1)
      .jsonPath("$.top[0].alias").isEqualTo("Ghast Sabio 113")
      .jsonPath("$.top[0].xp").isEqualTo(900)
      .jsonPath("$.top[0].esTu").isEqualTo(false)
      .jsonPath("$.yo").isEmpty();
  }

  @Test
  void rankingConBearerValidoPreguntaPorElUsuario() {
    when(jwt.authenticate(any())).thenReturn(Mono.just(new UsernamePasswordAuthenticationToken(
      new AuthenticatedUser(9L, "STUDENT", "gems-abierto"), "tok", List.of())));
    when(taps.ranking(any(), any(), any(), any(), eq(9L))).thenReturn(Mono.just(new TapsRankingUseCase.Ranking("semana",
      "2026-10-19T05:00:00Z", "2026-10-26T05:00:00Z", 0, List.of(), null)));

    client.get().uri("/api/v1/public/taps/ranking").header(HttpHeaders.AUTHORIZATION, "Bearer tok").exchange()
      .expectStatus().isOk();
    verify(taps).ranking(null, null, null, null, 9L);
  }

  @Test
  void rankingPasaLosFiltros() {
    when(taps.ranking("hoy", "logica", ID, 10, null)).thenReturn(Mono.just(new TapsRankingUseCase.Ranking("hoy",
      "2026-10-21T05:00:00Z", "2026-10-22T05:00:00Z", 45, List.of(),
      new TapsRankingUseCase.Yo(7, 340, 94, "Creeper Veloz 482"))));

    client.get().uri("/api/v1/public/taps/ranking?periodo=hoy&tema=logica&jugadorId=" + ID + "&limite=10").exchange()
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.yo.puesto").isEqualTo(7)
      .jsonPath("$.yo.percentil").isEqualTo(94)
      .jsonPath("$.yo.alias").isEqualTo("Creeper Veloz 482");
  }
}
