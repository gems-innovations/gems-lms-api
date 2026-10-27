package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.shared.security.SecurityExceptionAdvice;
import io.r2dbc.spi.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Institution requests with the character draft of /crear (PR-9a): what is stored and who sees it. */
class PublicCatalogControllerRequestsTest {

  static final String DRAFT = "{\"v\":1,\"borradorId\":\"mh3k2a9f-0abc123\",\"b\":\"grabador\","
    + "\"l\":{\"skin\":\"#c99a76\",\"hairStyle\":\"corto\"},\"i\":\"pluma\",\"w\":\"taller-londres\"}";

  private final DatabaseClient db = mock(DatabaseClient.class);
  private final DatabaseClient.GenericExecuteSpec spec = mock(DatabaseClient.GenericExecuteSpec.class, RETURNS_SELF);
  private final Row row = mock(Row.class);
  private PublicCatalogController controller;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    stubRows();
    controller = new PublicCatalogController(mock(CourseGateway.class), db, "gems-abierto");
  }

  @SuppressWarnings("unchecked")
  private void stubRows() {
    when(db.sql(anyString())).thenReturn(spec);
    when(row.get("id", Long.class)).thenReturn(7L);
    when(spec.map(any(BiFunction.class))).thenAnswer(inv -> {
      BiFunction<Row, Object, Object> f = inv.getArgument(0);
      RowsFetchSpec<Object> rows = mock(RowsFetchSpec.class);
      when(rows.one()).thenAnswer(i -> Mono.just(f.apply(row, null)));
      when(rows.all()).thenAnswer(i -> Flux.just(f.apply(row, null)));
      return rows;
    });
  }

  private WebTestClient anonymous() {
    return WebTestClient.bindToController(controller)
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice()).build();
  }

  private WebTestClient.ResponseSpec post(String json) {
    return anonymous().post().uri("/api/v1/public/institution-requests").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(json).exchange();
  }

  private static String body(String message, String draft) {
    return "{\"institutionName\":\"Colegio Sol\",\"contactName\":\"Ana\",\"email\":\"ana@sol.edu\""
      + (message == null ? "" : ",\"message\":" + message) + (draft == null ? "" : ",\"draft\":" + draft) + "}";
  }

  private String boundDraft() {
    ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
    verify(spec, atLeast(0)).bind(eq("draft"), value.capture());
    if (!value.getAllValues().isEmpty()) return (String) value.getAllValues().get(0);
    verify(spec).bindNull("draft", String.class);
    return null;
  }

  @Test
  void guardaElBorradorValidoYNoLoDevuelveAlPublico() {
    post(body("\"Queremos un espacio\"", "\"" + DRAFT.replace("\"", "\\\"") + "\"")).expectStatus().isCreated()
      .expectBody().jsonPath("$.id").isEqualTo(7).jsonPath("$.status").isEqualTo("received")
      .jsonPath("$.draft").doesNotExist();
    assertEquals(DRAFT, boundDraft());
    verify(spec).bind("message", "Queremos un espacio");
  }

  @Test
  void unBorradorInvalidoNoPierdeLaSolicitud() {
    post(body("\"Hola\"", "\"{no es json\"")).expectStatus().isCreated();
    assertNull(boundDraft());
    verify(spec).bind("message", "Hola");
  }

  @Test
  void unBorradorFalsificadoSeDescarta() {
    String forged = DRAFT.replace("grabador", "<img src=x onerror=alert(1)>").replace("\"}", "\",\"admin\":true}");
    post(body(null, "\"" + forged.replace("\"", "\\\"") + "\"")).expectStatus().isCreated();
    assertNull(boundDraft());
    verify(spec).bindNull("message", String.class);
  }

  @Test
  void unBorradorEnormeSeDescartaYElMensajeQuedaIntacto() {
    // Well-formed and valid, but padded past 2048 bytes: rejected by size alone.
    String huge = DRAFT.replace("\"v\":1,", "\"v\":1," + " ".repeat(2048));
    post(body("\"Hola\"", "\"" + huge.replace("\"", "\\\"") + "\"")).expectStatus().isCreated();
    assertNull(boundDraft());
    verify(spec).bind("message", "Hola");
  }

  @Test
  void elBorradorPuedeLlegarComoObjetoYSeGuardaCanonico() {
    String unordered = "{\"w\":\"taller-londres\",\"i\":\"pluma\",\"l\":{\"skin\":\"#c99a76\",\"hairStyle\":\"corto\"},"
      + "\"b\":\"grabador\",\"borradorId\":\"mh3k2a9f-0abc123\",\"v\":1}";
    post(body("\"Hola\"", unordered)).expectStatus().isCreated();
    assertEquals(DRAFT, boundDraft());
    verify(spec).bind("message", "Hola");
  }

  @Test
  void otrosTiposEnDraftNuncaFallanLaSolicitud() {
    for (String raw : new String[] {"{}", "[1]", "123", "null", "true", "{\"v\":1,\"x\":[[[]]]}"}) {
      reset(spec);
      stubRows();
      // A fresh controller per value: the public endpoint allows 5 requests a day per client.
      controller = new PublicCatalogController(mock(CourseGateway.class), db, "gems-abierto");
      post(body("\"Hola\"", raw)).expectStatus().isCreated().expectBody().jsonPath("$.id").isEqualTo(7);
      assertNull(boundDraft(), raw);
      verify(spec).bind("message", "Hola");
    }
  }

  @Test
  void elBloqueDentroDelMensajeSeGuardaYElMensajeQuedaIntacto() {
    String message = "Hola\\n\\n[gems-borrador v1]\\n" + DRAFT.replace("\"", "\\\"") + "\\n[/gems-borrador]";
    post(body("\"" + message + "\"", null)).expectStatus().isCreated();
    assertEquals(DRAFT, boundDraft());
    verify(spec).bind("message", "Hola\n\n[gems-borrador v1]\n" + DRAFT + "\n[/gems-borrador]");
  }

  @Test
  void unMundoNuevoDelCreadorSeGuardaYUnoDesconocidoQuedaNulo() {
    String aula = DRAFT.replace("taller-londres", "aula");
    post(body("\"Hola\"", aula)).expectStatus().isCreated();
    assertEquals(aula, boundDraft());
    reset(spec);
    stubRows();
    controller = new PublicCatalogController(mock(CourseGateway.class), db, "gems-abierto");
    post(body("\"Hola\"", DRAFT.replace("taller-londres", "planetario"))).expectStatus().isCreated();
    assertNull(boundDraft());
    verify(spec).bind("message", "Hola");
  }

  @Test
  void sinBorradorSeGuardaNulo() {
    post(body("\"Solo mensaje\"", null)).expectStatus().isCreated();
    assertNull(boundDraft());
  }

  @Test
  void elSuperAdminVeElBorradorComoTextoEnElListado() {
    when(row.get("institution_name", String.class)).thenReturn("Colegio Sol");
    when(row.get("message", String.class)).thenReturn("Hola");
    when(row.get("draft", String.class)).thenReturn(DRAFT);
    when(row.get("status", String.class)).thenReturn("new");
    when(row.get("created_at", LocalDateTime.class)).thenReturn(LocalDateTime.of(2026, 10, 25, 12, 0));
    WebTestClient.bindToController(controller).webFilter(TestSecurity.superAdmin())
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice()).build()
      .get().uri("/api/v1/institution-requests").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$[0].id").isEqualTo(7).jsonPath("$[0].message").isEqualTo("Hola")
      .jsonPath("$[0].draft").isEqualTo(DRAFT);
  }

  @Test
  void elListadoNoEsPublico() {
    anonymous().get().uri("/api/v1/institution-requests").exchange().expectStatus().isForbidden();
  }
}
