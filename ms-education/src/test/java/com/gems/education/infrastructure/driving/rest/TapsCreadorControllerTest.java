package com.gems.education.infrastructure.driving.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gems.education.application.TapsCreadorUseCase;
import com.gems.education.application.TapsPayloadValidador;
import com.gems.education.application.TapsPayloadValidadorTest;
import com.gems.education.application.gateway.TapsCreadorGateway;
import com.gems.education.application.gateway.TapsCreadorGateway.Tap;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.SecurityExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * El controller con el caso de uso y el validador reales y la base simulada: así se prueban de verdad los roles, la
 * propiedad (404 para lo ajeno), los límites y la validación del contenido tal como los ve el cliente.
 */
class TapsCreadorControllerTest extends ControllerTestSupport {
  private static final Instant AHORA = Instant.parse("2026-10-26T15:00:00Z");
  private static final String ID = "u-bcdfghjkmn";
  private static final String AJENO = "u-xxxxxxxxxx";

  private final TapsCreadorGateway gateway = mock(TapsCreadorGateway.class);
  private final TapsPayloadValidador validador = TapsPayloadValidador.delClasspath();
  private TapsCreadorController controller;

  private Tap fila(Long owner, int revision) {
    return new Tap(ID, owner, null, "Roma antigua", "private", "borrador", 0, revision,
      validador.validar(TapsPayloadValidadorTest.tapValido()).json(), null, null, AHORA, AHORA, null);
  }

  @BeforeEach
  void setUp() {
    controller = new TapsCreadorController(new TapsCreadorUseCase(gateway, validador, Clock.fixed(AHORA, ZoneOffset.UTC),
      new com.gems.shared.web.ClientQuota(60, 60)), mapper);
    lenient().when(gateway.contarVivos(anyLong())).thenReturn(Mono.just(0L));
    lenient().when(gateway.contarCreadosDesde(anyLong(), any())).thenReturn(Mono.just(0L));
    lenient().when(gateway.crear(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    // Solo la creadora (8) tiene el Tap; para cualquier otro dueño la base no devuelve nada.
    lenient().when(gateway.buscar(anyString(), anyLong())).thenReturn(Mono.empty());
    lenient().when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, 2)));
    lenient().when(gateway.buscarEnRevision(anyString())).thenReturn(Mono.empty());
    lenient().when(gateway.listar(anyLong(), any())).thenReturn(Flux.empty());
    lenient().when(gateway.listar(8L, null)).thenReturn(Flux.just(fila(8L, 2)));
    lenient().when(gateway.actualizar(anyString(), anyLong(), any(), anyString(), anyString(), any())).thenReturn(Mono.empty());
    lenient().when(gateway.actualizar(eq(ID), eq(8L), isNull(), anyString(), anyString(), any())).thenReturn(Mono.just(fila(8L, 3)));
    lenient().when(gateway.actualizar(eq(ID), eq(8L), eq(2), anyString(), anyString(), any())).thenReturn(Mono.just(fila(8L, 3)));
    lenient().when(gateway.borrar(anyString(), anyLong(), any())).thenReturn(Mono.just(false));
    lenient().when(gateway.borrar(ID, 8L, AHORA)).thenReturn(Mono.just(true));
  }

  private WebTestClient como(AuthenticatedUser quien) {
    return client(controller, quien);
  }

  private static String tap() {
    return TapsPayloadValidadorTest.tapValido().toString();
  }

  @Test
  void sinSesionEs403EnTodasLasRutas() {
    WebTestClient anonimo = WebTestClient.bindToController(controller)
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice()).build();
    anonimo.get().uri("/api/v1/taps").exchange().expectStatus().isForbidden();
    anonimo.post().uri("/api/v1/taps").contentType(MediaType.APPLICATION_JSON).bodyValue(tap()).exchange().expectStatus().isForbidden();
    anonimo.get().uri("/api/v1/taps/" + ID).exchange().expectStatus().isForbidden();
    anonimo.put().uri("/api/v1/taps/" + ID).contentType(MediaType.APPLICATION_JSON).bodyValue(tap()).exchange().expectStatus().isForbidden();
    anonimo.delete().uri("/api/v1/taps/" + ID).exchange().expectStatus().isForbidden();
    verifyNoInteractions(gateway);
  }

  @Test
  void unEstudianteRecibe403YNoTocaLaBase() {
    como(STUDENT).get().uri("/api/v1/taps").exchange().expectStatus().isForbidden();
    como(STUDENT).post().uri("/api/v1/taps").contentType(MediaType.APPLICATION_JSON).bodyValue(tap()).exchange().expectStatus().isForbidden();
    como(STUDENT).get().uri("/api/v1/taps/" + ID).exchange().expectStatus().isForbidden();
    como(STUDENT).put().uri("/api/v1/taps/" + ID).contentType(MediaType.APPLICATION_JSON).bodyValue(tap()).exchange().expectStatus().isForbidden();
    como(STUDENT).delete().uri("/api/v1/taps/" + ID).exchange().expectStatus().isForbidden();
    verifyNoInteractions(gateway);
  }

  @Test
  void creadorDocenteAdminYSuperadminPuedenCrear() {
    for (AuthenticatedUser quien : new AuthenticatedUser[] {CREATOR, INSTRUCTOR, ADMIN, SUPER_ADMIN}) {
      como(quien).post().uri("/api/v1/taps").contentType(MediaType.APPLICATION_JSON).bodyValue(tap()).exchange()
        .expectStatus().isCreated()
        .expectHeader().valueEquals(HttpHeaders.ETAG, "\"0\"")
        .expectHeader().cacheControl(org.springframework.http.CacheControl.noStore())
        .expectBody()
        .jsonPath("$.id").value(org.hamcrest.Matchers.matchesRegex("^u-[23456789bcdfghjkmnpqrstvwxz]{10}$"))
        .jsonPath("$.estado").isEqualTo("borrador")
        .jsonPath("$.visibilidad").isEqualTo("private")
        .jsonPath("$.revision").isEqualTo(0)
        .jsonPath("$.titulo").isEqualTo("Roma antigua")
        .jsonPath("$.borrador.topic").isEqualTo("creador")
        .jsonPath("$.borrador.questions[0].id").isEqualTo("p1")
        .jsonPath("$.ownerId").doesNotExist();
    }
    verify(gateway, times(4)).crear(any());
  }

  @Test
  void elContenidoInvalidoEs400ConMensajeYSinGuardar() {
    ObjectNode malo = TapsPayloadValidadorTest.tapValido();
    malo.put("title", "Ve a www.estafa.com");
    como(CREATOR).post().uri("/api/v1/taps").contentType(MediaType.APPLICATION_JSON).bodyValue(malo.toString()).exchange()
      .expectStatus().isBadRequest()
      .expectBody().jsonPath("$.code").isEqualTo("VALIDATION_ERROR").jsonPath("$.message").value(org.hamcrest.Matchers.containsString("enlaces"));
    como(CREATOR).post().uri("/api/v1/taps").contentType(MediaType.APPLICATION_JSON).bodyValue("{no es json").exchange()
      .expectStatus().isBadRequest();
    como(CREATOR).post().uri("/api/v1/taps").contentType(MediaType.APPLICATION_JSON).bodyValue("[1,2]").exchange()
      .expectStatus().isBadRequest();
    verify(gateway, never()).crear(any());
  }

  @Test
  void laCuotaResponde429() {
    when(gateway.contarVivos(8L)).thenReturn(Mono.just(30L));
    como(CREATOR).post().uri("/api/v1/taps").contentType(MediaType.APPLICATION_JSON).bodyValue(tap()).exchange()
      .expectStatus().isEqualTo(429).expectBody().jsonPath("$.code").isEqualTo("TAPS_TOO_MANY_GAMES");
  }

  @Test
  void misTapsSoloTraeLosDelDueno() {
    como(CREATOR).get().uri("/api/v1/taps").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.length()").isEqualTo(1).jsonPath("$[0].id").isEqualTo(ID);
    como(INSTRUCTOR).get().uri("/api/v1/taps").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.length()").isEqualTo(0);
    verify(gateway).listar(8L, null);
    verify(gateway).listar(3L, null);
    como(CREATOR).get().uri("/api/v1/taps?estado=cualquiera").exchange().expectStatus().isBadRequest();
  }

  @Test
  void elDuenoLeeSuTapConEtagYLosDemasReciben404() {
    como(CREATOR).get().uri("/api/v1/taps/" + ID).exchange().expectStatus().isOk()
      .expectHeader().valueEquals(HttpHeaders.ETAG, "\"2\"")
      .expectBody().jsonPath("$.id").isEqualTo(ID).jsonPath("$.borrador.title").isEqualTo("Roma antigua");
    // Mismo id, otro usuario (incluso ADMIN o SUPER_ADMIN): 404, nunca 403.
    for (AuthenticatedUser otro : new AuthenticatedUser[] {INSTRUCTOR, ADMIN, SUPER_ADMIN}) {
      como(otro).get().uri("/api/v1/taps/" + ID).exchange().expectStatus().isNotFound()
        .expectBody().jsonPath("$.code").isEqualTo("TAP_NOT_FOUND");
      verify(gateway).buscar(ID, otro.userId());
    }
    como(CREATOR).get().uri("/api/v1/taps/" + AJENO).exchange().expectStatus().isNotFound();
    // Un id que no tiene forma de Tap de usuario ni llega a la base.
    como(CREATOR).get().uri("/api/v1/taps/mc-gancho").exchange().expectStatus().isNotFound();
    verify(gateway, never()).buscar(eq("mc-gancho"), anyLong());
  }

  @Test
  void guardarRespetaDuenoIfMatchYValidacion() {
    como(CREATOR).put().uri("/api/v1/taps/" + ID).contentType(MediaType.APPLICATION_JSON).bodyValue(tap()).exchange()
      .expectStatus().isOk().expectHeader().valueEquals(HttpHeaders.ETAG, "\"3\"")
      .expectBody().jsonPath("$.revision").isEqualTo(3);
    como(CREATOR).put().uri("/api/v1/taps/" + ID).header(HttpHeaders.IF_MATCH, "\"2\"").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(tap()).exchange().expectStatus().isOk();
    como(CREATOR).put().uri("/api/v1/taps/" + ID).header(HttpHeaders.IF_MATCH, "\"1\"").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(tap()).exchange().expectStatus().isEqualTo(409)
      .expectBody().jsonPath("$.code").isEqualTo("TAP_REVISION_CONFLICT");
    como(CREATOR).put().uri("/api/v1/taps/" + ID).header(HttpHeaders.IF_MATCH, "abc").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(tap()).exchange().expectStatus().isBadRequest();
    // Ajeno: 404 y la base nunca recibe el UPDATE.
    como(ADMIN).put().uri("/api/v1/taps/" + ID).contentType(MediaType.APPLICATION_JSON).bodyValue(tap()).exchange()
      .expectStatus().isNotFound();
    verify(gateway, never()).actualizar(anyString(), eq(2L), any(), anyString(), anyString(), any());
    // Contenido inválido: 400 sin tocar la base.
    ObjectNode malo = TapsPayloadValidadorTest.tapValido();
    ((ObjectNode) malo.get("scene")).put("host", "sofi");
    como(CREATOR).put().uri("/api/v1/taps/" + ID).contentType(MediaType.APPLICATION_JSON).bodyValue(malo.toString()).exchange()
      .expectStatus().isBadRequest();
    // Tres UPDATE del dueño (sin If-Match, con "2" y con "1"); ni el If-Match inválido ni el contenido inválido llegan.
    verify(gateway, times(3)).actualizar(eq(ID), eq(8L), any(), anyString(), anyString(), any());
  }

  @Test
  void borrarEsDelDuenoYLosDemasReciben404() {
    como(CREATOR).delete().uri("/api/v1/taps/" + ID).exchange().expectStatus().isNoContent();
    como(SUPER_ADMIN).delete().uri("/api/v1/taps/" + ID).exchange().expectStatus().isNotFound();
    verify(gateway).borrar(ID, 8L, AHORA);
    verify(gateway).borrar(ID, 1L, AHORA);
  }

  // ── A3: publicar, despublicar, cola y decisión ───────────────────────────

  private Tap fila(Long owner, String estado, String visibilidad, int revision, String publicado) {
    return new Tap(ID, owner, null, "Roma antigua", visibilidad, estado, 1, revision,
      validador.validar(TapsPayloadValidadorTest.tapValido()).json(), publicado, null, AHORA, AHORA, AHORA);
  }

  @Test
  void publicarEsDirectoParaDocentesYARevisionParaCreadores() {
    String json = validador.validar(TapsPayloadValidadorTest.tapValido()).json();
    when(gateway.buscar(ID, 3L)).thenReturn(Mono.just(fila(3L, 2)));
    when(gateway.publicar(eq(ID), eq(3L), eq(2), eq("enlace"), eq(json), any()))
      .thenReturn(Mono.just(fila(3L, "publicado", "enlace", 3, json)));
    como(INSTRUCTOR).post().uri("/api/v1/taps/" + ID + "/publicar").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"visibilidad\":\"enlace\"}").exchange().expectStatus().isOk()
      .expectHeader().valueEquals(HttpHeaders.ETAG, "\"3\"")
      .expectBody().jsonPath("$.estado").isEqualTo("publicado").jsonPath("$.visibilidad").isEqualTo("enlace")
      .jsonPath("$.publicado.title").isEqualTo("Roma antigua").jsonPath("$.ownerId").doesNotExist();

    when(gateway.enviarRevision(eq(ID), eq(8L), eq(2), any())).thenReturn(Mono.just(fila(8L, "en_revision", "enlace", 3, null)));
    como(CREATOR).post().uri("/api/v1/taps/" + ID + "/publicar").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.estado").isEqualTo("en_revision").jsonPath("$.publicado").doesNotExist();
    verify(gateway, never()).publicar(eq(ID), eq(8L), any(), anyString(), anyString(), any());

    // Ajeno: 404; estudiante: 403; visibilidad desconocida: 400; If-Match viejo: 409.
    como(ADMIN).post().uri("/api/v1/taps/" + ID + "/publicar").exchange().expectStatus().isNotFound();
    como(STUDENT).post().uri("/api/v1/taps/" + ID + "/publicar").exchange().expectStatus().isForbidden();
    como(INSTRUCTOR).post().uri("/api/v1/taps/" + ID + "/publicar").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"visibilidad\":\"institucion\"}").exchange().expectStatus().isBadRequest();
    como(INSTRUCTOR).post().uri("/api/v1/taps/" + ID + "/publicar").header(HttpHeaders.IF_MATCH, "\"1\"").exchange()
      .expectStatus().isEqualTo(409).expectBody().jsonPath("$.code").isEqualTo("TAP_REVISION_CONFLICT");
    verify(gateway, times(1)).publicar(anyString(), anyLong(), any(), anyString(), anyString(), any());
  }

  @Test
  void publicarUnoEnRevisionEs409YDespublicarLoRetira() {
    when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, "en_revision", "enlace", 3, null)));
    como(CREATOR).post().uri("/api/v1/taps/" + ID + "/publicar").exchange().expectStatus().isEqualTo(409)
      .expectBody().jsonPath("$.code").isEqualTo("TAP_ESTADO_INVALIDO");
    when(gateway.despublicar(eq(ID), eq(8L), any())).thenReturn(Mono.just(fila(8L, "borrador", "private", 4, null)));
    como(CREATOR).post().uri("/api/v1/taps/" + ID + "/despublicar").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.estado").isEqualTo("borrador");
    when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, 4)));
    como(CREATOR).post().uri("/api/v1/taps/" + ID + "/despublicar").exchange().expectStatus().isEqualTo(409);
    como(SUPER_ADMIN).post().uri("/api/v1/taps/" + ID + "/despublicar").exchange().expectStatus().isNotFound();
    verify(gateway, times(1)).despublicar(anyString(), anyLong(), any());
  }

  @Test
  void laColaYLaDecisionSonSoloDelSuperadmin() {
    when(gateway.listarEnRevision()).thenReturn(Flux.just(fila(8L, "en_revision", "enlace", 3, null)));
    when(gateway.buscarEnRevision(ID)).thenReturn(Mono.just(fila(8L, "en_revision", "enlace", 3, null)));
    como(SUPER_ADMIN).get().uri("/api/v1/taps?revision=1").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$[0].id").isEqualTo(ID).jsonPath("$[0].ownerId").isEqualTo(8).jsonPath("$[0].estado").isEqualTo("en_revision");
    como(SUPER_ADMIN).get().uri("/api/v1/taps/" + ID).exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.ownerId").isEqualTo(8).jsonPath("$.borrador.title").isEqualTo("Roma antigua");
    for (AuthenticatedUser otro : new AuthenticatedUser[] {ADMIN, INSTRUCTOR, CREATOR, STUDENT}) {
      como(otro).get().uri("/api/v1/taps?revision=1").exchange().expectStatus().isForbidden();
      como(otro).post().uri("/api/v1/taps/" + ID + "/revision").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"decision\":\"aprobar\"}").exchange().expectStatus().isForbidden();
    }
    // ADMIN e INSTRUCTOR siguen sin ver el Tap de la cola: no es suyo.
    como(ADMIN).get().uri("/api/v1/taps/" + ID).exchange().expectStatus().isNotFound();
    verify(gateway, times(1)).listarEnRevision();
    verify(gateway, never()).aprobar(anyString(), anyInt(), anyString(), anyLong(), any());

    String json = validador.validar(TapsPayloadValidadorTest.tapValido()).json();
    when(gateway.aprobar(eq(ID), eq(3), eq(json), eq(1L), any())).thenReturn(Mono.just(fila(8L, "publicado", "enlace", 4, json)));
    como(SUPER_ADMIN).post().uri("/api/v1/taps/" + ID + "/revision").header(HttpHeaders.IF_MATCH, "\"3\"")
      .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"decision\":\"aprobar\"}").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.estado").isEqualTo("publicado").jsonPath("$.version").isEqualTo(1);
    como(SUPER_ADMIN).post().uri("/api/v1/taps/" + ID + "/revision").header(HttpHeaders.IF_MATCH, "\"2\"")
      .contentType(MediaType.APPLICATION_JSON).bodyValue("{\"decision\":\"aprobar\"}").exchange().expectStatus().isEqualTo(409);
    como(SUPER_ADMIN).post().uri("/api/v1/taps/" + ID + "/revision").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"decision\":\"rechazar\"}").exchange().expectStatus().isBadRequest();
    como(SUPER_ADMIN).post().uri("/api/v1/taps/" + ID + "/revision").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"decision\":\"publicar\",\"motivo\":\"x\"}").exchange().expectStatus().isBadRequest();
    when(gateway.rechazar(eq(ID), eq(3), eq("Falta una explicación"), eq(1L), any()))
      .thenReturn(Mono.just(fila(8L, "rechazado", "private", 4, null)));
    como(SUPER_ADMIN).post().uri("/api/v1/taps/" + ID + "/revision").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"decision\":\"cambios\",\"motivo\":\"Falta una explicación\"}").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.estado").isEqualTo("rechazado");
    // Lo que no está en la cola (aunque exista como borrador de alguien): 404.
    when(gateway.buscarEnRevision(ID)).thenReturn(Mono.empty());
    como(SUPER_ADMIN).post().uri("/api/v1/taps/" + ID + "/revision").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"decision\":\"aprobar\"}").exchange().expectStatus().isNotFound();
  }

  @Test
  void ifMatchSeLeeConOSinComillasYDebil() {
    org.junit.jupiter.api.Assertions.assertNull(TapsCreadorController.revisionDe(null));
    org.junit.jupiter.api.Assertions.assertNull(TapsCreadorController.revisionDe("  "));
    org.junit.jupiter.api.Assertions.assertEquals(7, TapsCreadorController.revisionDe("7"));
    org.junit.jupiter.api.Assertions.assertEquals(7, TapsCreadorController.revisionDe("\"7\""));
    org.junit.jupiter.api.Assertions.assertEquals(7, TapsCreadorController.revisionDe("W/\"7\""));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> TapsCreadorController.revisionDe("*"));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> TapsCreadorController.revisionDe("9999999999"));
  }
}
