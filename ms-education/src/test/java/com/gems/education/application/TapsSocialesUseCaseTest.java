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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsSocialesUseCaseTest {
  private static final Long YO = 5L;
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
    StepVerifier.create(caso.cambiar(YO, "mc-gancho", true))
      .expectNext(new TapsSocialesUseCase.Estado("mc-gancho", true, 3)).verifyComplete();
    StepVerifier.create(caso.cambiar(YO, "mc-gancho", true))
      .expectNext(new TapsSocialesUseCase.Estado("mc-gancho", true, 3)).verifyComplete();
    verify(gateway, times(2)).ponerMeGusta("mc-gancho", YO);

    when(gateway.meGusta(any())).thenReturn(Mono.just(Map.of()));
    StepVerifier.create(caso.cambiar(YO, "mc-gancho", false))
      .expectNext(new TapsSocialesUseCase.Estado("mc-gancho", false, 0)).verifyComplete();
    verify(gateway).quitarMeGusta("mc-gancho", YO);
  }

  @Test
  void rechazaTapsFueraDeLaListaBlancaYPeticionesIncompletas() {
    for (String tap : List.of("no-existe", "u-bcdfghjkmn", "MC'; DROP", "")) {
      StepVerifier.create(caso.cambiar(YO, tap, true)).expectError(IllegalArgumentException.class).verify();
    }
    StepVerifier.create(caso.cambiar(YO, "mc-gancho", null)).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(caso.cambiar(null, "mc-gancho", true)).expectError(IllegalArgumentException.class).verify();
    verify(gateway, never()).ponerMeGusta(any(), any());
  }

  @Test
  void limitaLosCambiosPorUsuarioYMinuto() {
    Instant[] ahora = {Instant.parse("2026-10-24T15:00:00Z")};
    Clock movil = new Clock() {
      public ZoneId getZone() { return ZoneOffset.UTC; }
      public Clock withZone(ZoneId z) { return this; }
      public Instant instant() { return ahora[0]; }
    };
    caso = new TapsSocialesUseCase(gateway, registrados, movil);
    for (int i = 0; i < TapsSocialesUseCase.CAMBIOS_POR_MINUTO; i++) {
      StepVerifier.create(caso.cambiar(YO, "mc-gancho", i % 2 == 0)).expectNextCount(1).verifyComplete();
    }
    StepVerifier.create(caso.cambiar(YO, "mc-gancho", true)).expectError(TapsLimiteException.class).verify();
    StepVerifier.create(caso.cambiar(6L, "mc-gancho", true)).expectNextCount(1).verifyComplete();   // otro usuario
    ahora[0] = ahora[0].plus(Duration.ofSeconds(61));
    StepVerifier.create(caso.cambiar(YO, "mc-gancho", true)).expectNextCount(1).verifyComplete();
  }

  @Test
  void siElLimiteSeLlenaLosNuevosPasan() {
    LimitePorMinuto limite = new LimitePorMinuto(1, reloj);
    for (long i = 0; i < LimitePorMinuto.MAX_VIGILADOS; i++) limite.turno(1000 + i);
    assertTrue(limite.turno(1L));
    assertTrue(limite.turno(1L));   // sin vigilar
    assertFalse(limite.turno(1000L));   // los vigilados siguen limitados
  }

  @Test
  void leerDevuelveCifrasEnOrdenEIgnoraIdsInvalidos() {
    when(gateway.vistas(any(), any(), any())).thenReturn(Mono.just(Map.of("fleming", 40L, "mc-gancho", 7L)));
    when(gateway.meGusta(any())).thenReturn(Mono.just(Map.of("fleming", 2L)));
    when(gateway.comentarios(any())).thenReturn(Mono.just(Map.of("mc-gancho", 4L)));
    StepVerifier.create(caso.leer("fleming, no-existe,mc-gancho,fleming,,X Y"))
      .assertNext(s -> assertEquals(List.of(new TapsSocialesUseCase.Social("fleming", 40, 2, 0),
        new TapsSocialesUseCase.Social("mc-gancho", 7, 0, 4)), s.taps()))
      .verifyComplete();
    verify(gateway).vistas(List.of("fleming", "mc-gancho"), LocalDate.parse("2026-09-25"), LocalDate.parse("2026-10-24"));
  }

  @Test
  void miosDiceQueLeGustoAlUsuario() {
    when(gateway.meGustaDe(eq(YO), any())).thenReturn(Mono.just(Set.of("mc-gancho")));
    StepVerifier.create(caso.mios(YO, "mc-gancho,fleming,no-existe"))
      .assertNext(s -> assertEquals(List.of(new TapsSocialesUseCase.MeGustaYo("mc-gancho", true),
        new TapsSocialesUseCase.MeGustaYo("fleming", false)), s.taps()))
      .verifyComplete();
  }

  @Test
  void leerConsultaComoMucho20TapsDeCreador() {
    List<String> buscados = new java.util.ArrayList<>();
    caso = new TapsSocialesUseCase(gateway, id -> { buscados.add(id); return Mono.empty(); }, reloj);
    when(gateway.vistas(any(), any(), any())).thenReturn(Mono.just(Map.of()));
    when(gateway.comentarios(any())).thenReturn(Mono.just(Map.of()));
    String ids = IntStream.range(0, 25).mapToObj(i -> "u-bcdfghjk2" + "23456789bcdfghjkmnpqrstvwxz".charAt(i))
      .collect(Collectors.joining(",")) + ",mc-gancho," + "x;y,".repeat(100);
    StepVerifier.create(caso.leer(ids)).assertNext(s -> assertTrue(s.taps().isEmpty())).verifyComplete();
    assertEquals(21, buscados.size());
  }

  @Test
  void leerRechazaMasDe60Ids() {
    String muchos = IntStream.range(0, 61).mapToObj(i -> "t" + i).collect(Collectors.joining(","));
    StepVerifier.create(caso.leer(muchos)).expectError(IllegalArgumentException.class).verify();
  }
}
