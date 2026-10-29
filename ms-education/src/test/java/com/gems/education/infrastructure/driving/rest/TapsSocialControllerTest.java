package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsSocialUseCase;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.JwtReactiveAuthenticationManager;
import com.gems.shared.security.SecurityExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsSocialControllerTest {
  private final TapsSocialUseCase social = mock(TapsSocialUseCase.class);
  private final JwtReactiveAuthenticationManager jwt = mock(JwtReactiveAuthenticationManager.class);
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToController(new TapsSocialController(social, jwt))
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice())
      .build();
    lenient().when(jwt.authenticate(any())).thenReturn(Mono.just(new UsernamePasswordAuthenticationToken(
      new AuthenticatedUser(9L, "STUDENT", "gems-abierto"), "tok", List.of())));
  }

  @Test
  void comentarBorrarYReportarSinSesionDan401() {
    client.post().uri("/api/v1/public/taps/mc-gancho/comentarios").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"texto\":\"hola\"}").exchange().expectStatus().isUnauthorized();
    client.delete().uri("/api/v1/public/taps/mc-gancho/comentarios/1").exchange().expectStatus().isUnauthorized();
    client.post().uri("/api/v1/public/taps/mc-gancho/comentarios/1/reportar").exchange().expectStatus().isUnauthorized();
    verifyNoInteractions(social);
  }

  @Test
  void conSesionComentaComoElUsuario() {
    when(social.comentar(eq("mc-gancho"), any(), eq(9L)))
      .thenReturn(Mono.just(new TapsSocialUseCase.Comentario(5, "Creeper Veloz 482", "hola", "2026-10-29T20:00:00Z", true)));
    client.post().uri("/api/v1/public/taps/mc-gancho/comentarios").header(HttpHeaders.AUTHORIZATION, "Bearer tok")
      .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"texto\":\"hola\"}")
      .exchange().expectStatus().isCreated()
      .expectBody().jsonPath("$.alias").isEqualTo("Creeper Veloz 482").jsonPath("$.mio").isEqualTo(true);
  }

  @Test
  void borrarAjenoDa404() {
    when(social.borrar("mc-gancho", 1L, 9L)).thenReturn(Mono.just(false));
    client.delete().uri("/api/v1/public/taps/mc-gancho/comentarios/1").header(HttpHeaders.AUTHORIZATION, "Bearer tok")
      .exchange().expectStatus().isNotFound();
  }

  @Test
  void likeSinSesionUsaElDispositivoYUnTextoMaloDa400() {
    when(social.like("mc-gancho", true, null, "abc")).thenReturn(Mono.error(new IllegalArgumentException("jugadorId debe ser un UUID")));
    client.put().uri("/api/v1/public/taps/mc-gancho/like?jugadorId=abc").exchange().expectStatus().isBadRequest();
  }
}
