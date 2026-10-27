package com.gems.education.application;

import com.gems.education.application.gateway.TapsCreadorGateway;
import com.gems.education.application.gateway.TapsCreadorGateway.Publicado;
import com.gems.education.application.gateway.TapsEstadisticasGateway;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsRegistradosTest {
  private static final Instant T0 = Instant.parse("2026-10-26T15:00:00Z");
  private static final String PUBLICADO = "u-bcdfghjkmn";
  private static final String PRIVADO = "u-pqrstvwxz2";
  private final TapsBanco banco = new TapsBanco(Map.of("mc-gancho", new TapsBanco.Tap("mc-gancho", "minecraft", 5)));
  private final TapsCreadorGateway creador = mock(TapsCreadorGateway.class);

  private TapsRegistrados.Compuesto registrados(Clock reloj) {
    when(creador.buscarPublicado(anyString())).thenReturn(Mono.empty());
    when(creador.buscarPublicado(PUBLICADO)).thenReturn(Mono.just(new Publicado(PUBLICADO, 1, "{}", T0)));
    return new TapsRegistrados.Compuesto(banco, creador, reloj, Duration.ofSeconds(60));
  }

  @Test
  void bancoMasPublicadosYNadaMas() {
    TapsRegistrados r = registrados(Clock.fixed(T0, ZoneOffset.UTC));
    StepVerifier.create(r.buscar("mc-gancho")).assertNext(t -> assertEquals("minecraft", t.tema())).verifyComplete();
    StepVerifier.create(r.buscar(PUBLICADO)).assertNext(t -> {
      assertEquals(TapsRegistrados.TEMA_CREADOR, t.tema());
      assertEquals(5, t.total());
    }).verifyComplete();
    StepVerifier.create(r.buscar(PRIVADO)).verifyComplete();
    StepVerifier.create(r.buscar("napoleon-falso")).verifyComplete();
    StepVerifier.create(r.buscar(null)).verifyComplete();
    // El banco no consulta la base; un id que no tiene forma u- tampoco.
    verify(creador, never()).buscarPublicado("mc-gancho");
    verify(creador, never()).buscarPublicado("napoleon-falso");
  }

  @Test
  void laCacheGuardaPositivosYNegativosDuranteSuVida() {
    AtomicReference<Instant> ahora = new AtomicReference<>(T0);
    Clock reloj = new Clock() {
      public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
      public Clock withZone(java.time.ZoneId zone) { return this; }
      public Instant instant() { return ahora.get(); }
    };
    TapsRegistrados r = registrados(reloj);
    for (int i = 0; i < 3; i++) {
      StepVerifier.create(r.buscar(PUBLICADO)).expectNextCount(1).verifyComplete();
      StepVerifier.create(r.buscar(PRIVADO)).verifyComplete();
    }
    verify(creador, times(1)).buscarPublicado(PUBLICADO);
    verify(creador, times(1)).buscarPublicado(PRIVADO);
    ahora.set(T0.plusSeconds(61));
    // Pasado el minuto se vuelve a preguntar: un Tap retirado deja de contar en menos de un minuto.
    when(creador.buscarPublicado(PUBLICADO)).thenReturn(Mono.empty());
    StepVerifier.create(r.buscar(PUBLICADO)).verifyComplete();
    verify(creador, times(2)).buscarPublicado(PUBLICADO);
  }

  @Test
  void lasEstadisticasCuentanUnTapPublicadoYNoUnoRetirado() {
    TapsEstadisticasGateway estadisticas = mock(TapsEstadisticasGateway.class);
    when(estadisticas.sumar(any(), any(), any(), anyInt())).thenReturn(Mono.empty());
    TapsEstadisticasUseCase caso = new TapsEstadisticasUseCase(estadisticas, registrados(Clock.fixed(T0, ZoneOffset.UTC)),
      Clock.fixed(T0, ZoneOffset.UTC));
    StepVerifier.create(caso.registrar(new TapsEstadisticasUseCase.Evento(PUBLICADO, "vista", null, null))).verifyComplete();
    verify(estadisticas).sumar(PUBLICADO, LocalDate.parse("2026-10-26"), "vista", 0);
    StepVerifier.create(caso.registrar(new TapsEstadisticasUseCase.Evento(PRIVADO, "vista", null, null)))
      .expectError(IllegalArgumentException.class).verify();
    verify(estadisticas, never()).sumar(eq(PRIVADO), any(), any(), anyInt());

    when(estadisticas.leer(any(), any())).thenReturn(Flux.just(
      new TapsEstadisticasGateway.Fila(PUBLICADO, "vista", 0, 7),
      new TapsEstadisticasGateway.Fila(PRIVADO, "vista", 0, 99),
      new TapsEstadisticasGateway.Fila("mc-gancho", "vista", 0, 3)));
    StepVerifier.create(caso.leer(7)).assertNext(r -> {
      assertEquals(2, r.taps().size(), "el retirado no aparece ni con su id");
      assertEquals(PUBLICADO, r.masVisto());
      assertEquals(TapsRegistrados.TEMA_CREADOR, r.taps().get(0).tema());
      assertTrue(r.taps().stream().noneMatch(t -> t.tapId().equals(PRIVADO)));
    }).verifyComplete();
  }
}
