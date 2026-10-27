package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsPayloadValidador;
import com.gems.education.application.TapsPayloadValidadorTest;
import com.gems.education.application.gateway.TapsCreadorGateway;
import com.gems.education.application.gateway.TapsCreadorGateway.Publicado;
import com.gems.shared.security.SecurityExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** El endpoint público solo entrega la copia publicada: el filtro de estado vive en la base, aquí se comprueba lo que sale. */
class TapsPublicoControllerTest extends ControllerTestSupport {
  private static final String PUBLICADO = "u-bcdfghjkmn";
  private static final String PRIVADO = "u-pqrstvwxz2";
  private final TapsCreadorGateway gateway = mock(TapsCreadorGateway.class);
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    String json = TapsPayloadValidador.delClasspath().validar(TapsPayloadValidadorTest.tapValido()).json();
    when(gateway.buscarPublicado(anyString())).thenReturn(Mono.empty());
    when(gateway.buscarPublicado(PUBLICADO)).thenReturn(Mono.just(new Publicado(PUBLICADO, 3, json, Instant.parse("2026-10-26T15:00:00Z"))));
    client = WebTestClient.bindToController(new TapsPublicoController(gateway, mapper))
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice()).build();
  }

  @Test
  void unTapPublicadoSeEntregaSinSesionSinDuenoYConCache() {
    client.get().uri("/api/v1/public/taps/u/" + PUBLICADO).exchange().expectStatus().isOk()
      .expectHeader().valueEquals(HttpHeaders.ETAG, "\"3\"")
      .expectHeader().valueEquals(HttpHeaders.CACHE_CONTROL, "max-age=30, public")
      .expectBody()
      .jsonPath("$.id").isEqualTo(PUBLICADO)
      .jsonPath("$.version").isEqualTo(3)
      .jsonPath("$.soloEnlace").isEqualTo(true)
      .jsonPath("$.title").isEqualTo("Roma antigua")
      .jsonPath("$.questions.length()").isEqualTo(5)
      .jsonPath("$.ownerId").doesNotExist()
      .jsonPath("$.owner_id").doesNotExist()
      .jsonPath("$.institutionId").doesNotExist()
      .jsonPath("$.borrador").doesNotExist()
      .jsonPath("$.motivo").doesNotExist()
      .jsonPath("$.estado").doesNotExist()
      .jsonPath("$.email").doesNotExist();
  }

  @Test
  void loNoPublicadoYLoInexistenteSon404Iguales() {
    client.get().uri("/api/v1/public/taps/u/" + PRIVADO).exchange().expectStatus().isNotFound()
      .expectBody().jsonPath("$.code").isEqualTo("TAP_NOT_FOUND");
    client.get().uri("/api/v1/public/taps/u/u-2222222222").exchange().expectStatus().isNotFound()
      .expectBody().jsonPath("$.code").isEqualTo("TAP_NOT_FOUND");
    // Un id sin forma de Tap de usuario no llega a la base.
    client.get().uri("/api/v1/public/taps/u/mc-gancho").exchange().expectStatus().isNotFound();
    client.get().uri("/api/v1/public/taps/u/u-ABCDEFGHIJ").exchange().expectStatus().isNotFound();
    verify(gateway, never()).buscarPublicado("mc-gancho");
    verify(gateway, never()).buscarPublicado("u-ABCDEFGHIJ");
    verify(gateway).buscarPublicado(PRIVADO);
  }
}
