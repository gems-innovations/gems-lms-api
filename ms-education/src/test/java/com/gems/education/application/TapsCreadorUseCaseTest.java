package com.gems.education.application;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gems.education.application.exceptions.TapsCreadorConflictoException;
import com.gems.education.application.exceptions.TapsCreadorNotFoundException;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsCreadorGateway;
import com.gems.education.application.gateway.TapsCreadorGateway.Tap;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.web.ClientQuota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsCreadorUseCaseTest {
  private static final Instant AHORA = Instant.parse("2026-10-26T15:00:00Z");
  private static final AuthenticatedUser CREADORA = new AuthenticatedUser(8L, "CREATOR", null);
  private static final AuthenticatedUser DOCENTE = new AuthenticatedUser(3L, "INSTRUCTOR", "inst-1");
  private static final String ID = "u-bcdfghjkmn";

  private final TapsCreadorGateway gateway = mock(TapsCreadorGateway.class);
  private final TapsPayloadValidador validador = TapsPayloadValidador.delClasspath();
  private TapsCreadorUseCase caso;

  private static Tap fila(Long owner, int revision) {
    String json = TapsPayloadValidador.delClasspath().validar(TapsPayloadValidadorTest.tapValido()).json();
    return new Tap(ID, owner, null, "Roma antigua", "private", "borrador", 0, revision, json, null, null, AHORA, AHORA, null);
  }

  @BeforeEach
  void setUp() {
    caso = new TapsCreadorUseCase(gateway, validador, Clock.fixed(AHORA, ZoneOffset.UTC), new ClientQuota(3, 60));
    when(gateway.contarVivos(anyLong())).thenReturn(Mono.just(0L));
    when(gateway.contarCreadosDesde(anyLong(), any())).thenReturn(Mono.just(0L));
    when(gateway.crear(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
  }

  @Test
  void crearGuardaElCanonicoConDuenoInstitucionYEstadoBorrador() {
    StepVerifier.create(caso.crear(DOCENTE, TapsPayloadValidadorTest.tapValido()))
      .assertNext(t -> {
        assertTrue(TapsIds.esDeUsuario(t.id()));
        assertEquals(3L, t.ownerId());
        assertEquals("inst-1", t.institutionId());
        assertEquals("borrador", t.estado());
        assertEquals("private", t.visibilidad());
        assertEquals("Roma antigua", t.titulo());
        assertTrue(t.borrador().contains("\"topic\":\"creador\""));
      }).verifyComplete();
    verify(gateway).contarCreadosDesde(3L, AHORA.minusSeconds(86_400));
  }

  @Test
  void crearRechazaContenidoInvalidoAntesDeTocarLaBase() {
    ObjectNode malo = TapsPayloadValidadorTest.tapValido();
    malo.put("title", "<b>x</b>");
    assertThrows(IllegalArgumentException.class, () -> caso.crear(CREADORA, malo));
    verifyNoInteractions(gateway);
  }

  @Test
  void cuotaDeTapsVivos() {
    when(gateway.contarVivos(8L)).thenReturn(Mono.just((long) TapsCreadorUseCase.MAX_VIVOS));
    StepVerifier.create(caso.crear(CREADORA, TapsPayloadValidadorTest.tapValido())).expectError(TapsLimiteException.class).verify();
    verify(gateway, never()).crear(any());
  }

  @Test
  void cuotaDeCreadosPorDia() {
    when(gateway.contarCreadosDesde(eq(8L), any())).thenReturn(Mono.just((long) TapsCreadorUseCase.MAX_CREADOS_POR_DIA));
    StepVerifier.create(caso.crear(CREADORA, TapsPayloadValidadorTest.tapValido())).expectError(TapsLimiteException.class).verify();
    verify(gateway, never()).crear(any());
  }

  @Test
  void siElIdChocaReintentaConOtro() {
    doReturn(Mono.empty()).doAnswer(inv -> Mono.just(inv.getArgument(0))).when(gateway).crear(any());
    StepVerifier.create(caso.crear(CREADORA, TapsPayloadValidadorTest.tapValido())).expectNextCount(1).verifyComplete();
    ArgumentCaptor<Tap> c = ArgumentCaptor.forClass(Tap.class);
    verify(gateway, times(2)).crear(c.capture());
    assertNotEquals(c.getAllValues().get(0).id(), c.getAllValues().get(1).id());
  }

  @Test
  void siElIdChocaSiempreFallaTrasLosIntentos() {
    doReturn(Mono.empty()).when(gateway).crear(any());
    StepVerifier.create(caso.crear(CREADORA, TapsPayloadValidadorTest.tapValido())).expectError(IllegalStateException.class).verify();
    verify(gateway, times(TapsCreadorUseCase.INTENTOS_ID)).crear(any());
  }

  @Test
  void buscarPreguntaSiempreConElDuenoYUnAjenoEs404() {
    when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, 0)));
    when(gateway.buscar(ID, 3L)).thenReturn(Mono.empty());
    StepVerifier.create(caso.buscar(CREADORA, ID)).expectNextCount(1).verifyComplete();
    StepVerifier.create(caso.buscar(DOCENTE, ID)).expectError(TapsCreadorNotFoundException.class).verify();
  }

  @Test
  void unIdQueNoTieneFormaDeTapDeUsuarioEs404SinConsultar() {
    StepVerifier.create(caso.buscar(CREADORA, "mc-gancho")).expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.buscar(CREADORA, "u-' OR 1=1 --")).expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.borrar(CREADORA, "x")).expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.actualizar(CREADORA, "x", null, TapsPayloadValidadorTest.tapValido()))
      .expectError(TapsCreadorNotFoundException.class).verify();
    verifyNoInteractions(gateway);
  }

  @Test
  void listarFiltraPorEstadoConocido() {
    when(gateway.listar(8L, "borrador")).thenReturn(Flux.just(fila(8L, 0)));
    StepVerifier.create(caso.listar(CREADORA, "borrador")).expectNextCount(1).verifyComplete();
    StepVerifier.create(caso.listar(CREADORA, "DROP TABLE")).expectError(IllegalArgumentException.class).verify();
    verify(gateway, never()).listar(anyLong(), eq("DROP TABLE"));
  }

  @Test
  void actualizarValidaCompruebaDuenoYRespetaIfMatch() {
    when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, 2)));
    when(gateway.actualizar(eq(ID), eq(8L), eq(2), anyString(), anyString(), eq(AHORA))).thenReturn(Mono.just(fila(8L, 3)));
    when(gateway.actualizar(eq(ID), eq(8L), eq(1), anyString(), anyString(), eq(AHORA))).thenReturn(Mono.empty());

    StepVerifier.create(caso.actualizar(CREADORA, ID, 2, TapsPayloadValidadorTest.tapValido()))
      .assertNext(t -> assertEquals(3, t.revision())).verifyComplete();
    StepVerifier.create(caso.actualizar(CREADORA, ID, 1, TapsPayloadValidadorTest.tapValido()))
      .expectError(TapsCreadorConflictoException.class).verify();

    when(gateway.buscar(ID, 3L)).thenReturn(Mono.empty());
    StepVerifier.create(caso.actualizar(DOCENTE, ID, null, TapsPayloadValidadorTest.tapValido()))
      .expectError(TapsCreadorNotFoundException.class).verify();
    verify(gateway, never()).actualizar(any(), eq(3L), any(), any(), any(), any());

    ObjectNode malo = TapsPayloadValidadorTest.tapValido();
    malo.put("theme", "minecraft");
    assertThrows(IllegalArgumentException.class, () -> caso.actualizar(CREADORA, ID, null, malo));
  }

  @Test
  void demasiadosGuardadosSeguidosDanLimite() {
    when(gateway.buscar(ID, 8L)).thenReturn(Mono.just(fila(8L, 0)));
    when(gateway.actualizar(eq(ID), eq(8L), isNull(), anyString(), anyString(), any())).thenReturn(Mono.just(fila(8L, 1)));
    for (int i = 0; i < 3; i++) {
      StepVerifier.create(caso.actualizar(CREADORA, ID, null, TapsPayloadValidadorTest.tapValido())).expectNextCount(1).verifyComplete();
    }
    StepVerifier.create(caso.actualizar(CREADORA, ID, null, TapsPayloadValidadorTest.tapValido()))
      .expectError(TapsLimiteException.class).verify();
    // La cuota es por usuario: otro creador no se ve afectado.
    when(gateway.buscar(ID, 3L)).thenReturn(Mono.just(fila(3L, 0)));
    when(gateway.actualizar(eq(ID), eq(3L), isNull(), anyString(), anyString(), any())).thenReturn(Mono.just(fila(3L, 1)));
    StepVerifier.create(caso.actualizar(DOCENTE, ID, null, TapsPayloadValidadorTest.tapValido())).expectNextCount(1).verifyComplete();
  }

  @Test
  void borrarEsLogicoYUnAjenoEs404() {
    when(gateway.borrar(ID, 8L, AHORA)).thenReturn(Mono.just(true));
    when(gateway.borrar(ID, 3L, AHORA)).thenReturn(Mono.just(false));
    StepVerifier.create(caso.borrar(CREADORA, ID)).verifyComplete();
    StepVerifier.create(caso.borrar(DOCENTE, ID)).expectError(TapsCreadorNotFoundException.class).verify();
  }

  // ── Importar el borrador de /crear (A4) ───────────────────────────────────

  private static final String BORRADOR_ID = "mh3k2a9f-0abc123";

  private static ObjectNode adjunto(String base, String item, String mundo) {
    ObjectNode a = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().createObjectNode();
    a.put("v", 1).put("borradorId", BORRADOR_ID).put("b", base);
    a.set("l", a.objectNode().put("hairStyle", "corto").put("skin", "#f1c27d"));
    if (item == null) a.putNull("i"); else a.put("i", item);
    a.put("w", mundo);
    return a;
  }

  @Test
  void importarCreaMiPrimerTapPrivadoConLaEscenaDelAdjunto() {
    when(gateway.buscarPorOrigen(8L, BORRADOR_ID)).thenReturn(Mono.empty());
    when(gateway.crearImportado(any(), eq(BORRADOR_ID))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    StepVerifier.create(caso.importarBorrador(CREADORA, BORRADOR_ID, adjunto("senador", "pergamino", "foro-roma")))
      .assertNext(r -> {
        assertTrue(r.importado());
        Tap t = r.tap();
        assertTrue(TapsIds.esDeUsuario(t.id()));
        assertEquals(8L, t.ownerId());
        assertEquals("private", t.visibilidad());
        assertEquals("borrador", t.estado());
        assertEquals(TapsPrimerTap.TITULO, t.titulo());
        assertTrue(t.borrador().contains("\"scene\":{\"world\":\"foro-roma\",\"host\":\"senador\",\"item\":\"pergamino\"}"), t.borrador());
        assertTrue(t.borrador().contains("\"topic\":\"creador\""));
        // Vuelve a pasar por el validador sin cambios: es contenido canónico.
        assertEquals(t.borrador(), validador.validar(t.borrador()).json());
      }).verifyComplete();
    verify(gateway, never()).crear(any());
  }

  @Test
  void importarConSorpresaUsaElAnfitrionPorDefectoYSinObjeto() {
    when(gateway.buscarPorOrigen(8L, BORRADOR_ID)).thenReturn(Mono.empty());
    when(gateway.crearImportado(any(), eq(BORRADOR_ID))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    StepVerifier.create(caso.importarBorrador(CREADORA, null, adjunto("sorpresa", null, "biblioteca")))
      .assertNext(r -> assertTrue(r.tap().borrador().contains("\"scene\":{\"world\":\"biblioteca\",\"host\":\"grabador\"}"), r.tap().borrador()))
      .verifyComplete();
  }

  @Test
  void importarEsIdempotenteDevuelveElExistenteSinCrearNiGastarCuota() {
    when(gateway.buscarPorOrigen(8L, BORRADOR_ID)).thenReturn(Mono.just(fila(8L, 4)));
    StepVerifier.create(caso.importarBorrador(CREADORA, BORRADOR_ID, adjunto("senador", null, "foro-roma")))
      .assertNext(r -> {
        assertFalse(r.importado());
        assertEquals(ID, r.tap().id());
      }).verifyComplete();
    verify(gateway, never()).crearImportado(any(), anyString());
    verify(gateway, never()).contarVivos(anyLong());
  }

  @Test
  void importarSoloBuscaConElDuenoAutenticado() {
    when(gateway.buscarPorOrigen(anyLong(), anyString())).thenReturn(Mono.empty());
    when(gateway.crearImportado(any(), anyString())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    StepVerifier.create(caso.importarBorrador(DOCENTE, BORRADOR_ID, adjunto("senador", null, "foro-roma")))
      .assertNext(r -> {
        assertEquals(3L, r.tap().ownerId());
        assertEquals("inst-1", r.tap().institutionId());
      }).verifyComplete();
    verify(gateway).buscarPorOrigen(3L, BORRADOR_ID);
    verify(gateway, never()).buscarPorOrigen(eq(8L), anyString());
  }

  @Test
  void siDosPeticionesImportanALaVezLaSegundaDevuelveLaPrimera() {
    when(gateway.buscarPorOrigen(8L, BORRADOR_ID)).thenReturn(Mono.empty()).thenReturn(Mono.just(fila(8L, 0)));
    when(gateway.crearImportado(any(), eq(BORRADOR_ID))).thenReturn(Mono.empty());
    StepVerifier.create(caso.importarBorrador(CREADORA, BORRADOR_ID, adjunto("senador", null, "foro-roma")))
      .assertNext(r -> assertFalse(r.importado())).verifyComplete();
    verify(gateway, times(1)).crearImportado(any(), anyString());
  }

  @Test
  void siElIdChocaAlImportarReintentaConOtro() {
    when(gateway.buscarPorOrigen(8L, BORRADOR_ID)).thenReturn(Mono.empty());
    doReturn(Mono.empty()).doAnswer(inv -> Mono.just(inv.getArgument(0))).when(gateway).crearImportado(any(), eq(BORRADOR_ID));
    StepVerifier.create(caso.importarBorrador(CREADORA, BORRADOR_ID, adjunto("senador", null, "foro-roma")))
      .assertNext(r -> assertTrue(r.importado())).verifyComplete();
    verify(gateway, times(2)).crearImportado(any(), eq(BORRADOR_ID));
  }

  @Test
  void importarRespetaLasCuotasDeCrear() {
    when(gateway.buscarPorOrigen(8L, BORRADOR_ID)).thenReturn(Mono.empty());
    when(gateway.contarVivos(8L)).thenReturn(Mono.just((long) TapsCreadorUseCase.MAX_VIVOS));
    StepVerifier.create(caso.importarBorrador(CREADORA, BORRADOR_ID, adjunto("senador", null, "foro-roma")))
      .expectError(TapsLimiteException.class).verify();
    verify(gateway, never()).crearImportado(any(), anyString());
  }

  @Test
  void unAdjuntoInvalidoOUnIdQueNoCoincideEs404SinTocarLaBase() {
    ObjectNode fueraDeCatalogo = adjunto("senador", null, "marte");
    StepVerifier.create(caso.importarBorrador(CREADORA, BORRADOR_ID, fueraDeCatalogo)).expectError(TapsCreadorNotFoundException.class).verify();
    ObjectNode conClaveExtra = adjunto("senador", null, "foro-roma");
    conClaveExtra.put("ownerId", 3);
    StepVerifier.create(caso.importarBorrador(CREADORA, BORRADOR_ID, conClaveExtra)).expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.importarBorrador(CREADORA, "otro-id1234567", adjunto("senador", null, "foro-roma")))
      .expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.importarBorrador(CREADORA, BORRADOR_ID, null)).expectError(TapsCreadorNotFoundException.class).verify();
    StepVerifier.create(caso.importarBorrador(CREADORA, BORRADOR_ID, com.fasterxml.jackson.databind.node.TextNode.valueOf("{\"v\":1")))
      .expectError(TapsCreadorNotFoundException.class).verify();
    verifyNoInteractions(gateway);
  }

  @Test
  void elAdjuntoTambienPuedeVenirComoTextoJson() {
    when(gateway.buscarPorOrigen(8L, BORRADOR_ID)).thenReturn(Mono.empty());
    when(gateway.crearImportado(any(), eq(BORRADOR_ID))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    var texto = com.fasterxml.jackson.databind.node.TextNode.valueOf(adjunto("escriba", "libro", "aula").toString());
    StepVerifier.create(caso.importarBorrador(CREADORA, BORRADOR_ID, texto))
      .assertNext(r -> assertTrue(r.tap().borrador().contains("\"host\":\"escriba\",\"item\":\"libro\""))).verifyComplete();
  }
}
