package com.gems.education.application;

import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsSocialesGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsSocialesUseCaseTest {
  private static final String YO = "11111111-2222-3333-4444-555555555555";
  private final TapsSocialesGateway gateway = mock(TapsSocialesGateway.class);
  private final TapsRegistrados registrados = TapsRegistrados.deBanco(new TapsBanco(Map.of(
    "mc-gancho", new TapsBanco.Tap("mc-gancho", "minecraft", 5),
    "fleming", new TapsBanco.Tap("fleming", "historias", 1))));
  private final Clock reloj = Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), ZoneOffset.UTC);
  private TapsSocialesUseCase caso;

  @BeforeEach
  void setUp() {
    caso = new TapsSocialesUseCase(gateway, registrados, reloj);
    when(gateway.ponerMeGusta(any(), any())).thenReturn(Mono.empty());
    when(gateway.quitarMeGusta(any(), any())).thenReturn(Mono.empty());
    when(gateway.meGusta(any())).thenReturn(Mono.just(Map.of("mc-gancho", 3L)));
  }

  @Test
  void ponerYQuitarMandanElEstadoDeseadoYRespondenElTotal() {
    StepVerifier.create(caso.cambiar("mc-gancho", new TapsSocialesUseCase.Cambio(YO, true)))
      .expectNext(new TapsSocialesUseCase.Estado("mc-gancho", true, 3)).verifyComplete();
    StepVerifier.create(caso.cambiar("mc-gancho", new TapsSocialesUseCase.Cambio(YO, true)))
      .expectNext(new TapsSocialesUseCase.Estado("mc-gancho", true, 3)).verifyComplete();
    verify(gateway, times(2)).ponerMeGusta("mc-gancho", UUID.fromString(YO));

    when(gateway.meGusta(any())).thenReturn(Mono.just(Map.of()));
    StepVerifier.create(caso.cambiar("mc-gancho", new TapsSocialesUseCase.Cambio(YO, false)))
      .expectNext(new TapsSocialesUseCase.Estado("mc-gancho", false, 0)).verifyComplete();
    verify(gateway).quitarMeGusta("mc-gancho", UUID.fromString(YO));
  }

  @Test
  void rechazaTapsFueraDeLaListaBlancaYJugadoresInvalidos() {
    for (String tap : List.of("no-existe", "u-bcdfghjkmn", "MC'; DROP", "")) {
      StepVerifier.create(caso.cambiar(tap, new TapsSocialesUseCase.Cambio(YO, true))).expectError(IllegalArgumentException.class).verify();
    }
    for (String jugador : new String[]{null, "", "no-es-uuid", "1-2-3-4-5"}) {
      StepVerifier.create(caso.cambiar("mc-gancho", new TapsSocialesUseCase.Cambio(jugador, true)))
        .expectError(IllegalArgumentException.class).verify();
    }
    StepVerifier.create(caso.cambiar("mc-gancho", new TapsSocialesUseCase.Cambio(YO, null))).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(caso.cambiar("mc-gancho", null)).expectError(IllegalArgumentException.class).verify();
    verify(gateway, never()).ponerMeGusta(any(), any());
  }

  @Test
  void limitaLosCambiosPorJugadorYMinuto() {
    Instant[] ahora = {Instant.parse("2026-10-24T15:00:00Z")};
    Clock movil = new Clock() {
      public ZoneId getZone() { return ZoneOffset.UTC; }
      public Clock withZone(ZoneId z) { return this; }
      public Instant instant() { return ahora[0]; }
    };
    caso = new TapsSocialesUseCase(gateway, registrados, movil);
    for (int i = 0; i < TapsSocialesUseCase.CAMBIOS_POR_MINUTO; i++) {
      StepVerifier.create(caso.cambiar("mc-gancho", new TapsSocialesUseCase.Cambio(YO, i % 2 == 0))).expectNextCount(1).verifyComplete();
    }
    StepVerifier.create(caso.cambiar("mc-gancho", new TapsSocialesUseCase.Cambio(YO, true))).expectError(TapsLimiteException.class).verify();
    // Otro jugador no comparte el límite.
    StepVerifier.create(caso.cambiar("mc-gancho", new TapsSocialesUseCase.Cambio(UUID.randomUUID().toString(), true)))
      .expectNextCount(1).verifyComplete();
    ahora[0] = ahora[0].plus(Duration.ofSeconds(61));
    StepVerifier.create(caso.cambiar("mc-gancho", new TapsSocialesUseCase.Cambio(YO, true))).expectNextCount(1).verifyComplete();
  }

  @Test
  void leerDevuelveVistasYMeGustaEnOrdenEIgnoraIdsInvalidos() {
    when(gateway.vistas(any(), any(), any())).thenReturn(Mono.just(Map.of("fleming", 40L, "mc-gancho", 7L)));
    when(gateway.meGusta(any())).thenReturn(Mono.just(Map.of("fleming", 2L)));
    StepVerifier.create(caso.leer("fleming, no-existe,mc-gancho,fleming,,X Y", null))
      .assertNext(s -> assertEquals(List.of(new TapsSocialesUseCase.Social("fleming", 40, 2, null),
        new TapsSocialesUseCase.Social("mc-gancho", 7, 0, null)), s.taps()))
      .verifyComplete();
    verify(gateway).vistas(List.of("fleming", "mc-gancho"), LocalDate.parse("2026-09-25"), LocalDate.parse("2026-10-24"));
    verify(gateway, never()).meGustaDe(any(), any());
  }

  @Test
  void conJugadorDiceQueLeGusto() {
    when(gateway.vistas(any(), any(), any())).thenReturn(Mono.just(Map.of()));
    when(gateway.meGustaDe(eq(UUID.fromString(YO)), any())).thenReturn(Mono.just(Set.of("mc-gancho")));
    StepVerifier.create(caso.leer("mc-gancho,fleming", YO))
      .assertNext(s -> assertEquals(List.of(new TapsSocialesUseCase.Social("mc-gancho", 0, 3, true),
        new TapsSocialesUseCase.Social("fleming", 0, 0, false)), s.taps()))
      .verifyComplete();
  }

  @Test
  void siElLimiteSeLlenaConUuidsInventadosLosNuevosPasan() {
    for (int i = 0; i < TapsSocialesUseCase.MAX_VIGILADOS; i++) caso.turno(UUID.randomUUID());
    assertTrue(caso.turno(UUID.randomUUID()));
  }

  @Test
  void leerConsultaComoMucho20TapsDeCreadorEIgnoraLaBasura() {
    List<String> buscados = new java.util.ArrayList<>();
    caso = new TapsSocialesUseCase(gateway, id -> { buscados.add(id); return Mono.empty(); }, reloj);
    when(gateway.vistas(any(), any(), any())).thenReturn(Mono.just(Map.of()));
    String ids = java.util.stream.IntStream.range(0, 25).mapToObj(i -> "u-bcdfghjk2" + "23456789bcdfghjkmnpqrstvwxz".charAt(i))
      .collect(java.util.stream.Collectors.joining(",")) + ",mc-gancho," + "x;y,".repeat(100);
    StepVerifier.create(caso.leer(ids, null)).assertNext(s -> assertTrue(s.taps().isEmpty())).verifyComplete();
    assertEquals(21, buscados.size());
  }

  @Test
  void leerRechazaMasDe60IdsYJugadoresInvalidos() {
    String muchos = String.join(",", java.util.stream.IntStream.range(0, 61).mapToObj(i -> "t" + i).toList());
    StepVerifier.create(caso.leer(muchos, null)).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(caso.leer("mc-gancho", "no-es-uuid")).expectError(IllegalArgumentException.class).verify();
  }
}
