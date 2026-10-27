package com.gems.education.application;

import com.gems.education.application.exceptions.TapsCreadorConflictoException;
import com.gems.education.application.exceptions.TapsCreadorNotFoundException;
import com.gems.education.application.exceptions.TapsEstadoException;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsCreadorGateway;
import com.gems.education.application.gateway.TapsCreadorGateway.Tap;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.web.ClientQuota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Transiciones de estado de A3: publicar (directo o a revisión), despublicar y la decisión del superadmin. */
class TapsCreadorPublicarUseCaseTest {
  private static final Instant AHORA = Instant.parse("2026-10-26T15:00:00Z");
  private static final AuthenticatedUser CREADORA = new AuthenticatedUser(8L, "CREATOR", null);
  private static final AuthenticatedUser DOCENTE = new AuthenticatedUser(3L, "INSTRUCTOR", "inst-1");
  private static final AuthenticatedUser ADMIN = new AuthenticatedUser(2L, "ADMIN", "inst-1");
  private static final AuthenticatedUser SUPER = new AuthenticatedUser(1L, "SUPER_ADMIN", null);
  private static final String ID = "u-bcdfghjkmn";

  private final TapsCreadorGateway gateway = mock(TapsCreadorGateway.class);
  private final TapsPayloadValidador validador = TapsPayloadValidador.delClasspath();
  private final String json = validador.validar(TapsPayloadValidadorTest.tapValido()).json();
  private TapsCreadorUseCase caso;

  private Tap fila(Long owner, String estado, int revision) {
    return new Tap(ID, owner, null, "Roma antigua", "private", estado, 0, revision, json, null, null, AHORA, AHORA, null);
  }

  @BeforeEach
  void setUp() {
    caso = new TapsCreadorUseCase(gateway, validador, Clock.fixed(AHORA, ZoneOffset.UTC), new ClientQuota(60, 60), new ClientQuota(2, 86_400));
    lenient().when(gateway.buscar(anyString(), anyLong())).thenReturn(Mono.empty());
    lenient().when(gateway.buscarEnRevision(anyString())).thenReturn(Mono.empty());
    lenient().when(gateway.publicar(anyString(), anyLong(), any(), anyString(), anyString(), any()))
      .thenAnswer(inv -> Mono.just(fila(inv.getArgument(1), "publicado", (Integer) inv.getArgument(2) + 1)));
    lenient().when(gateway.enviarRevision(anyString(), anyLong(), any(), any()))
      .thenAnswer(inv -> Mono.just(fila(inv.getArgument(1), "en_revision", (Integer) inv.getArgument(2) + 1)));
    lenient().when(gateway.despublicar(anyString(), anyLong(), any()))
      .thenAnswer(inv -> Mono.just(fila(inv.getArgument(1), "borrador", 1)));
    lenient().when(gateway.aprobar(anyString(), anyInt(), anyString(), anyLong(), any()))
      .thenAnswer(inv -> Mono.just(fila(8L, "publicado", (Integer) inv.getArgument(1) + 1)));
    lenient().when(gateway.rechazar(anyString(), anyInt(), anyString(), anyLong(), any()))
      .thenAnswer(inv -> Mono.just(fila(8L, "rechazado", (Integer) inv.getArgument(1) + 1)));
  }

  @Test
  void docenteAdminYSuperadminPublicanConEnlaceDirecto() {
    for (AuthenticatedUser quien : new AuthenticatedUser[] {DOCENTE, ADMIN, SUPER}) {
      when(gateway.buscar(ID, quien.userId())).thenReturn(Mono.just(fila(quien.userId(), "borrador", 2)));
      StepVerifier.create(caso.publicar(quien, ID, "enlace", null))
        .assertNext(t -> assertEquals("publicado", t.estado())).verifyComplete();
      verify(gateway).publicar(eq(ID), eq(quien.userId()), eq(2), eq("enlace"), eq(json), eq(AHORA));
    }
    verify(gateway, never()).enviarRevision(anyString(), anyLong(), any(), any());
  }

  @Test
  void unCreatorConEnlaceVaARevisionYPrivadoEsDirecto() {
    when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, "borrador", 2)));
    StepVerifier.create(caso.publicar(CREADORA, ID, "enlace", null))
      .assertNext(t -> assertEquals("en_revision", t.estado())).verifyComplete();
    verify(gateway).enviarRevision(ID, 8L, 2, AHORA);
    verify(gateway, never()).publicar(anyString(), anyLong(), any(), anyString(), anyString(), any());

    StepVerifier.create(caso.publicar(CREADORA, ID, "private", null))
      .assertNext(t -> assertEquals("publicado", t.estado())).verifyComplete();
    verify(gateway).publicar(ID, 8L, 2, "private", json, AHORA);
  }

  @Test
  void enlaceEsLaVisibilidadPorDefectoYUnaDesconocidaEs400() {
    when(gateway.buscar(ID, 3L)).thenReturn(Mono.just(fila(3L, "borrador", 0)));
    StepVerifier.create(caso.publicar(DOCENTE, ID, null, null)).expectNextCount(1).verifyComplete();
    verify(gateway).publicar(ID, 3L, 0, "enlace", json, AHORA);
    StepVerifier.create(caso.publicar(DOCENTE, ID, "institucion", null)).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(caso.publicar(DOCENTE, ID, "public", null)).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  void cuotaDeEnviosARevisionPorDia() {
    when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, "rechazado", 4)));
    StepVerifier.create(caso.publicar(CREADORA, ID, "enlace", null)).expectNextCount(1).verifyComplete();
    StepVerifier.create(caso.publicar(CREADORA, ID, "enlace", null)).expectNextCount(1).verifyComplete();
    StepVerifier.create(caso.publicar(CREADORA, ID, "enlace", null)).expectError(TapsLimiteException.class).verify();
    verify(gateway, times(2)).enviarRevision(ID, 8L, 4, AHORA);
    // La cuota es solo para enviar a revisión: publicar privado sigue.
    StepVerifier.create(caso.publicar(CREADORA, ID, "private", null)).expectNextCount(1).verifyComplete();
  }

  @Test
  void publicarUnoEnRevisionEs409YUnAjenoOInexistente404() {
    when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, "en_revision", 3)));
    StepVerifier.create(caso.publicar(CREADORA, ID, "private", null)).expectError(TapsEstadoException.class).verify();
    StepVerifier.create(caso.publicar(DOCENTE, ID, "enlace", null)).expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.publicar(DOCENTE, "mc-gancho", "enlace", null)).expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.despublicar(DOCENTE, ID)).expectError(TapsCreadorNotFoundException.class).verify();
    verify(gateway, never()).publicar(anyString(), anyLong(), any(), anyString(), anyString(), any());
    verify(gateway, never()).enviarRevision(anyString(), anyLong(), any(), any());
    verify(gateway, never()).despublicar(anyString(), anyLong(), any());
  }

  @Test
  void publicarRespetaIfMatchYLaCarreraEnLaBase() {
    when(gateway.buscar(ID, 3L)).thenReturn(Mono.just(fila(3L, "borrador", 5)));
    StepVerifier.create(caso.publicar(DOCENTE, ID, "enlace", 4)).expectError(TapsCreadorConflictoException.class).verify();
    verify(gateway, never()).publicar(anyString(), anyLong(), any(), anyString(), anyString(), any());
    when(gateway.publicar(eq(ID), eq(3L), eq(5), anyString(), anyString(), any())).thenReturn(Mono.empty());
    StepVerifier.create(caso.publicar(DOCENTE, ID, "enlace", 5)).expectError(TapsCreadorConflictoException.class).verify();
  }

  @Test
  void publicarRevalidaElBorradorGuardado() {
    // Un borrador guardado antes de endurecer el validador (o manipulado) ya no pasa: 400 y nada se congela.
    Tap viejo = new Tap(ID, 3L, "inst-1", "Roma", "private", "borrador", 0, 1,
      json.replace("\"title\":\"Roma antigua\"", "\"title\":\"Entra a www.estafa.com\""), null, null, AHORA, AHORA, null);
    when(gateway.buscar(ID, 3L)).thenReturn(Mono.just(viejo));
    StepVerifier.create(caso.publicar(DOCENTE, ID, "enlace", null)).expectErrorSatisfies(e -> {
      assertInstanceOf(IllegalArgumentException.class, e);
      assertFalse(e.getMessage().contains("estafa"), "sin eco del contenido");
    }).verify();
    verify(gateway, never()).publicar(anyString(), anyLong(), any(), anyString(), anyString(), any());
  }

  @Test
  void despublicarVuelveABorradorYUnBorradorEs409() {
    when(gateway.buscar(ID, 3L)).thenReturn(Mono.just(fila(3L, "publicado", 2)));
    StepVerifier.create(caso.despublicar(DOCENTE, ID)).assertNext(t -> assertEquals("borrador", t.estado())).verifyComplete();
    verify(gateway).despublicar(ID, 3L, AHORA);
    when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, "en_revision", 2)));
    StepVerifier.create(caso.despublicar(CREADORA, ID)).expectNextCount(1).verifyComplete();
    when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, "borrador", 2)));
    StepVerifier.create(caso.despublicar(CREADORA, ID)).expectError(TapsEstadoException.class).verify();
  }

  @Test
  void elSuperadminVeLosDeLaColaYLaCola() {
    when(gateway.buscarEnRevision(ID)).thenReturn(Mono.just(fila(8L, "en_revision", 2)));
    StepVerifier.create(caso.buscar(SUPER, ID)).assertNext(t -> assertEquals(8L, t.ownerId())).verifyComplete();
    // Un ADMIN no: la cola es solo del superadmin, y buscar sigue siendo del dueño.
    StepVerifier.create(caso.buscar(ADMIN, ID)).expectError(TapsCreadorNotFoundException.class).verify();
    verify(gateway, times(1)).buscarEnRevision(ID);
    when(gateway.listarEnRevision()).thenReturn(Flux.just(fila(8L, "en_revision", 2)));
    StepVerifier.create(caso.colaRevision(SUPER)).expectNextCount(1).verifyComplete();
    StepVerifier.create(caso.colaRevision(ADMIN)).expectError(TapsCreadorNotFoundException.class).verify();
    verify(gateway, times(1)).listarEnRevision();
  }

  @Test
  void aprobarCongelaElBorradorRevalidadoConLaRevisionVista() {
    when(gateway.buscarEnRevision(ID)).thenReturn(Mono.just(fila(8L, "en_revision", 7)));
    StepVerifier.create(caso.decidir(SUPER, ID, "aprobar", null, 7))
      .assertNext(t -> assertEquals("publicado", t.estado())).verifyComplete();
    verify(gateway).aprobar(ID, 7, json, 1L, AHORA);
    StepVerifier.create(caso.decidir(SUPER, ID, "aprobar", null, 6)).expectError(TapsCreadorConflictoException.class).verify();
    when(gateway.aprobar(eq(ID), eq(7), anyString(), anyLong(), any())).thenReturn(Mono.empty());
    StepVerifier.create(caso.decidir(SUPER, ID, "aprobar", null, null)).expectError(TapsCreadorConflictoException.class).verify();
  }

  @Test
  void cambiosYRechazarExigenMotivoYLoGuardan() {
    when(gateway.buscarEnRevision(ID)).thenReturn(Mono.just(fila(8L, "en_revision", 7)));
    StepVerifier.create(caso.decidir(SUPER, ID, "rechazar", null, null)).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(caso.decidir(SUPER, ID, "cambios", "   ", null)).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(caso.decidir(SUPER, ID, "cambios", "  La pregunta 3  tiene\u0007 dos respuestas ", null))
      .expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(caso.decidir(SUPER, ID, "cambios", "x".repeat(301), null)).expectError(IllegalArgumentException.class).verify();
    verify(gateway, never()).rechazar(anyString(), anyInt(), anyString(), anyLong(), any());
    StepVerifier.create(caso.decidir(SUPER, ID, "cambios", "  La pregunta 3  tiene dos respuestas ", 7))
      .assertNext(t -> assertEquals("rechazado", t.estado())).verifyComplete();
    verify(gateway).rechazar(ID, 7, "La pregunta 3 tiene dos respuestas", 1L, AHORA);
    StepVerifier.create(caso.decidir(SUPER, ID, "borrar", "x", null)).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  void enUnaReRevisionCambiosORechazarNoTumbanLaCopiaAprobada() {
    // El caso de uso no decide la visibilidad: la pasa tal cual la deja la base (copia aprobada con enlace, sigue pública).
    Tap conCopia = new Tap(ID, 8L, null, "Roma antigua", "enlace", "en_revision", 1, 7, json, "{\"v\":2}", null, AHORA, AHORA, AHORA);
    when(gateway.buscarEnRevision(ID)).thenReturn(Mono.just(conCopia));
    when(gateway.rechazar(eq(ID), eq(7), anyString(), anyLong(), any())).thenReturn(Mono.just(
      new Tap(ID, 8L, null, "Roma antigua", "enlace", "rechazado", 1, 8, json, "{\"v\":2}", "Dos respuestas", AHORA, AHORA, AHORA)));
    for (String decision : List.of("cambios", "rechazar")) {
      StepVerifier.create(caso.decidir(SUPER, ID, decision, "Dos respuestas", 7)).assertNext(t -> {
        assertEquals("rechazado", t.estado());
        assertEquals("enlace", t.visibilidad(), "la copia aprobada sigue publicada");
        assertEquals("{\"v\":2}", t.publicado());
        assertEquals("Dos respuestas", t.motivo());
      }).verifyComplete();
    }
    verify(gateway, times(2)).rechazar(ID, 7, "Dos respuestas", 1L, AHORA);
  }

  @Test
  void soloElSuperadminDecideYLoQueNoEstaEnColaEs404() {
    when(gateway.buscar(ID, 2L)).thenReturn(Mono.just(fila(2L, "en_revision", 1)));
    StepVerifier.create(caso.decidir(ADMIN, ID, "aprobar", null, null)).expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.decidir(DOCENTE, ID, "aprobar", null, null)).expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.decidir(SUPER, ID, "aprobar", null, null)).expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.decidir(SUPER, "mc-gancho", "aprobar", null, null)).expectError(TapsCreadorNotFoundException.class).verify();
    verify(gateway, never()).aprobar(anyString(), anyInt(), anyString(), anyLong(), any());
    verify(gateway, never()).rechazar(anyString(), anyInt(), anyString(), anyLong(), any());
  }

  @Test
  void aprobarRevalidaYNoPublicaLoQueYaNoPasa() {
    Tap sucio = new Tap(ID, 8L, null, "Roma", "enlace", "en_revision", 0, 1,
      json.replace("\"title\":\"Roma antigua\"", "\"title\":\"<img src=x>\""), null, null, AHORA, AHORA, null);
    when(gateway.buscarEnRevision(ID)).thenReturn(Mono.just(sucio));
    StepVerifier.create(caso.decidir(SUPER, ID, "aprobar", null, null)).expectError(IllegalArgumentException.class).verify();
    verify(gateway, never()).aprobar(anyString(), anyInt(), anyString(), anyLong(), any());
  }

  @Test
  void constantesCoherentesConElPlan() {
    assertEquals(Set.of("borrador", "en_revision", "publicado", "rechazado"), TapsCreadorUseCase.ESTADOS);
    assertEquals(Set.of("private", "enlace"), TapsCreadorUseCase.VISIBILIDADES, "institucion queda para la fase 2");
    assertEquals(5, TapsCreadorUseCase.MAX_ENVIOS_REVISION_POR_DIA);
    assertNull(TapsCreadorUseCase.limpiarMotivo(null));
    assertNull(TapsCreadorUseCase.limpiarMotivo(" \n "));
  }
}
