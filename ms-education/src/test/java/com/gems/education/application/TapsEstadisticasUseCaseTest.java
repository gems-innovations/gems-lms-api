package com.gems.education.application;

import com.gems.education.application.gateway.TapsEstadisticasGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsEstadisticasUseCaseTest {
  private final TapsEstadisticasGateway gateway = mock(TapsEstadisticasGateway.class);
  private final TapsBanco banco = new TapsBanco(Map.of(
    "mc-gancho", new TapsBanco.Tap("mc-gancho", "minecraft", 5),
    "fleming", new TapsBanco.Tap("fleming", "historias", 1)));
  private final Clock reloj = Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), ZoneOffset.UTC);
  private TapsEstadisticasUseCase caso;

  @BeforeEach
  void setUp() {
    caso = new TapsEstadisticasUseCase(gateway, banco, reloj);
    when(gateway.sumar(any(), any(), any(), anyInt())).thenReturn(Mono.empty());
  }

  @Test
  void unaVistaSumaUnoAlContadorDelDia() {
    StepVerifier.create(caso.registrar(new TapsEstadisticasUseCase.Evento("mc-gancho", "vista", null, null))).verifyComplete();
    verify(gateway).sumar("mc-gancho", LocalDate.parse("2026-10-24"), "vista", 0);
  }

  @Test
  void unaRespuestaCorrectaSumaRespuestaYAcierto() {
    StepVerifier.create(caso.registrar(new TapsEstadisticasUseCase.Evento("mc-gancho", "respuesta", 2, true))).verifyComplete();
    verify(gateway).sumar("mc-gancho", LocalDate.parse("2026-10-24"), "respuesta", 2);
    verify(gateway).sumar("mc-gancho", LocalDate.parse("2026-10-24"), "acierto", 2);
  }

  @Test
  void unaRespuestaIncorrectaNoSumaAcierto() {
    StepVerifier.create(caso.registrar(new TapsEstadisticasUseCase.Evento("mc-gancho", "respuesta", 0, false))).verifyComplete();
    verify(gateway, never()).sumar(any(), any(), eq("acierto"), anyInt());
  }

  @Test
  void rechazaTapsEventosYPreguntasInvalidos() {
    assertThrows(IllegalArgumentException.class, () -> caso.registrar(new TapsEstadisticasUseCase.Evento("no-existe", "vista", null, null)));
    assertThrows(IllegalArgumentException.class, () -> caso.registrar(new TapsEstadisticasUseCase.Evento("mc-gancho", "hackeo", null, null)));
    assertThrows(IllegalArgumentException.class, () -> caso.registrar(new TapsEstadisticasUseCase.Evento("mc-gancho", "respuesta", 5, true)));
    assertThrows(IllegalArgumentException.class, () -> caso.registrar(new TapsEstadisticasUseCase.Evento("mc-gancho", "respuesta", null, true)));
    assertThrows(IllegalArgumentException.class, () -> caso.registrar(new TapsEstadisticasUseCase.Evento("fleming", "respuesta", 1, true)));
    assertThrows(IllegalArgumentException.class, () -> caso.registrar(null));
    verifyNoInteractions(gateway.getClass().cast(gateway));
  }

  @Test
  void resumenCalculaPrimeraRespuestaAbandonoYMasVisto() {
    when(gateway.leer(any(), any())).thenReturn(Flux.just(
      new TapsEstadisticasGateway.Fila("mc-gancho", "vista", 0, 100),
      new TapsEstadisticasGateway.Fila("mc-gancho", "respuesta", 0, 80), new TapsEstadisticasGateway.Fila("mc-gancho", "acierto", 0, 60),
      new TapsEstadisticasGateway.Fila("mc-gancho", "respuesta", 1, 70), new TapsEstadisticasGateway.Fila("mc-gancho", "acierto", 1, 30),
      new TapsEstadisticasGateway.Fila("mc-gancho", "respuesta", 2, 50), new TapsEstadisticasGateway.Fila("mc-gancho", "respuesta", 3, 45),
      new TapsEstadisticasGateway.Fila("mc-gancho", "respuesta", 4, 40), new TapsEstadisticasGateway.Fila("mc-gancho", "completado", 0, 38),
      new TapsEstadisticasGateway.Fila("mc-gancho", "compartido", 0, 5),
      new TapsEstadisticasGateway.Fila("fleming", "vista", 0, 20),
      new TapsEstadisticasGateway.Fila("desconocido", "vista", 0, 999)));
    StepVerifier.create(caso.leer(7)).assertNext(r -> {
      assertEquals(7, r.dias());
      assertEquals("2026-10-18", r.desde());
      assertEquals("2026-10-24", r.hasta());
      assertEquals("mc-gancho", r.masVisto());
      assertEquals(2, r.taps().size(), "el Tap desconocido no cuenta");
      TapsEstadisticasUseCase.TapEstadistica t = r.taps().get(0);
      assertEquals(100, t.vistas());
      assertEquals(80, t.primeraRespuesta());
      assertEquals(38, t.completados());
      assertEquals(5, t.compartidos());
      assertEquals(285, t.respuestas());
      assertEquals(90, t.aciertos());
      List<TapsEstadisticasUseCase.Pregunta> p = t.preguntas();
      assertEquals(5, p.size());
      assertEquals(10, p.get(0).abandonos());
      assertEquals(20, p.get(1).abandonos());
      assertEquals(5, p.get(2).abandonos());
      assertEquals(5, p.get(3).abandonos());
      assertEquals(2, p.get(4).abandonos(), "tras la última: respondieron 40 y completaron 38");
    }).verifyComplete();
  }

  @Test
  void losDiasSeAcotanEntre1Y90() {
    when(gateway.leer(any(), any())).thenReturn(Flux.empty());
    StepVerifier.create(caso.leer(500)).assertNext(r -> assertEquals(90, r.dias())).verifyComplete();
    StepVerifier.create(caso.leer(0)).assertNext(r -> assertEquals(1, r.dias())).verifyComplete();
    StepVerifier.create(caso.leer(null)).assertNext(r -> assertEquals(30, r.dias())).verifyComplete();
  }
}