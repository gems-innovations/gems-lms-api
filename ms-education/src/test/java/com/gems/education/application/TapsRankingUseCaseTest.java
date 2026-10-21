package com.gems.education.application;

import com.gems.education.application.TapsRankingUseCase.Partida;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsGateway;
import com.gems.education.application.gateway.TapsGateway.Fila;
import com.gems.education.application.gateway.TapsGateway.Identidad;
import com.gems.education.application.gateway.TapsGateway.Jugador;
import com.gems.education.application.gateway.TapsGateway.Posicion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsRankingUseCaseTest {
  /** Miércoles 21 de octubre de 2026, 15:00 en Bogotá (20:00 UTC). */
  private static final Instant AHORA = Instant.parse("2026-10-21T20:00:00Z");
  private static final String ID = "3f1c2b7a-9d4e-4c1a-8b2f-0a1b2c3d4e5f";
  private static final UUID UID = UUID.fromString(ID);
  private static final TapsBanco BANCO = new TapsBanco(Map.of(
    "mc-gancho", new TapsBanco.Tap("mc-gancho", "minecraft", 5),
    "lg-corto", new TapsBanco.Tap("lg-corto", "logica", 3)));

  private final TapsGateway gateway = mock(TapsGateway.class);
  private TapsRankingUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new TapsRankingUseCase(gateway, BANCO, Clock.fixed(AHORA, ZoneOffset.UTC), new Random(7));
    lenient().when(gateway.buscarJugador(any())).thenReturn(Mono.empty());
    lenient().when(gateway.crearJugador(any(), any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    lenient().when(gateway.vincularDispositivo(any(), any())).thenReturn(Mono.empty());
    lenient().when(gateway.aliasDeUsuario(any())).thenReturn(Mono.empty());
    lenient().when(gateway.crearUsuario(any(), anyString(), any())).thenAnswer(inv -> Mono.just(inv.getArgument(1)));
    lenient().when(gateway.reservarTurno(any(), any(), any(), anyInt())).thenReturn(Mono.just(true));
    lenient().when(gateway.repeticiones(any(), anyString(), any(), any())).thenReturn(Mono.just(0L));
    lenient().when(gateway.guardarPartida(any(), anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyLong(), any()))
      .thenReturn(Mono.empty());
    lenient().when(gateway.posicion(any(), any(), any(), any())).thenReturn(Mono.empty());
  }

  private static Partida partida(int aciertos, int total, long ms) {
    return new Partida(ID, "mc-gancho", "minecraft", aciertos, total, ms);
  }

  private void noSeGuardoNada() {
    verify(gateway, never()).guardarPartida(any(), anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyLong(), any());
  }

  // ── XP ─────────────────────────────────────────────────────────────────────

  @Test
  void xpEsDiezPorAciertoMasVeinteSiEsPerfecta() {
    assertEquals(40, TapsRankingUseCase.xpBase(4, 5, 41_000));
    assertEquals(70, TapsRankingUseCase.xpBase(5, 5, 41_000));
    assertEquals(0, TapsRankingUseCase.xpBase(0, 5, 41_000));
  }

  @Test
  void partidaDemasiadoRapidaNoDaXp() {
    assertEquals(0, TapsRankingUseCase.xpBase(5, 5, 7_499));
    assertEquals(70, TapsRankingUseCase.xpBase(5, 5, 7_500));
  }

  @Test
  void desdeLaCuartaRepeticionDaElVeinticincoPorCiento() {
    assertEquals(70, TapsRankingUseCase.xpConRepeticion(70, 2));
    assertEquals(17, TapsRankingUseCase.xpConRepeticion(70, 3));
    assertEquals(10, TapsRankingUseCase.xpConRepeticion(40, 5));
  }

  @Test
  void registrarAplicaLaRepeticionDeLaSemanaYGuardaLaPartida() {
    when(gateway.repeticiones(any(), eq("mc-gancho"), any(), any())).thenReturn(Mono.just(3L));

    StepVerifier.create(useCase.registrar(partida(5, 5, 41_000), null))
      .assertNext(r -> assertEquals(17, r.xpGanado()))
      .verifyComplete();
    verify(gateway).guardarPartida(argThat(i -> i.usuarioId() == null && UID.equals(i.jugadorId())),
      eq("mc-gancho"), eq("minecraft"), eq(5), eq(5), eq(17), eq(41_000L), eq(AHORA));
    verify(gateway).repeticiones(any(), eq("mc-gancho"),
      eq(Instant.parse("2026-10-19T05:00:00Z")), eq(Instant.parse("2026-10-26T05:00:00Z")));
  }

  @Test
  void registrarRapidoSeGuardaConCeroXp() {
    StepVerifier.create(useCase.registrar(partida(5, 5, 1_000), null))
      .assertNext(r -> assertEquals(0, r.xpGanado()))
      .verifyComplete();
    verify(gateway).guardarPartida(any(), eq("mc-gancho"), eq("minecraft"), eq(5), eq(5), eq(0), eq(1_000L), eq(AHORA));
  }

  // ── Semana y día de Bogotá ─────────────────────────────────────────────────

  @Test
  void semanaVaDeLunesALunesHoraDeBogota() {
    TapsRankingUseCase.Rango r = TapsRankingUseCase.semana(AHORA);
    assertEquals(Instant.parse("2026-10-19T05:00:00Z"), r.desde());
    assertEquals(Instant.parse("2026-10-26T05:00:00Z"), r.hasta());
  }

  @Test
  void domingoNocheEnBogotaAunEsLaSemanaAnterior() {
    // Lunes 26, 03:00 UTC = domingo 25, 22:00 en Bogotá.
    TapsRankingUseCase.Rango r = TapsRankingUseCase.semana(Instant.parse("2026-10-26T03:00:00Z"));
    assertEquals(Instant.parse("2026-10-19T05:00:00Z"), r.desde());
    // Lunes 26, 05:00 UTC = lunes 00:00 en Bogotá: empieza la nueva.
    assertEquals(Instant.parse("2026-10-26T05:00:00Z"), TapsRankingUseCase.semana(Instant.parse("2026-10-26T05:00:00Z")).desde());
  }

  @Test
  void hoyEsElDiaCalendarioDeBogota() {
    TapsRankingUseCase.Rango r = TapsRankingUseCase.hoy(Instant.parse("2026-10-22T02:00:00Z"));
    assertEquals(Instant.parse("2026-10-21T05:00:00Z"), r.desde());
    assertEquals(Instant.parse("2026-10-22T05:00:00Z"), r.hasta());
  }

  // ── Percentil y respuesta ──────────────────────────────────────────────────

  @Test
  void percentilCuentaSoloALosDemas() {
    // 36 de los otros 49 tienen menos XP.
    assertEquals(73, TapsRankingUseCase.percentil(36, 50));
    // El primero de 3 supera a los otros 2: 100, no 66.
    assertEquals(100, TapsRankingUseCase.percentil(2, 3));
    // El último nunca supera a nadie.
    assertEquals(0, TapsRankingUseCase.percentil(0, 120));
    // Jugando solo: 100.
    assertEquals(100, TapsRankingUseCase.percentil(0, 1));
    assertEquals(100, TapsRankingUseCase.percentil(0, 0));
  }

  @Test
  void registrarRespondeAliasSemanaYPercentilDeHoy() {
    when(gateway.posicion(any(), eq(Instant.parse("2026-10-19T05:00:00Z")), any(), isNull()))
      .thenReturn(Mono.just(new Posicion(7, 340, 120, 100, "x")));
    when(gateway.posicion(any(), eq(Instant.parse("2026-10-21T05:00:00Z")), any(), isNull()))
      .thenReturn(Mono.just(new Posicion(5, 60, 45, 32, "x")));

    StepVerifier.create(useCase.registrar(partida(4, 5, 41_000), null))
      .assertNext(r -> {
        assertEquals(40, r.xpGanado());
        assertEquals(new TapsRankingUseCase.Semana(7, 120, 340), r.semana());
        assertEquals(new TapsRankingUseCase.Hoy(72, 45), r.hoy());
        assertTrue(r.alias().matches("^\\p{L}+ \\p{L}+ [1-9]\\d{2}$"), r.alias());
      })
      .verifyComplete();
  }

  // ── Identidad y alias ──────────────────────────────────────────────────────

  @Test
  void elAliasDelDispositivoSeAsignaUnaVezYSeConserva() {
    when(gateway.buscarJugador(UID)).thenReturn(Mono.just(new Jugador(UID, null, "Creeper Veloz 482")));

    StepVerifier.create(useCase.registrar(partida(4, 5, 41_000), null))
      .assertNext(r -> assertEquals("Creeper Veloz 482", r.alias()))
      .verifyComplete();
    verify(gateway, never()).crearJugador(any(), any());
  }

  @Test
  void conSesionLaPartidaSumaAlUsuarioConSuAlias() {
    when(gateway.buscarJugador(UID)).thenReturn(Mono.just(new Jugador(UID, null, "Zorro Leal 111")));
    when(gateway.aliasDeUsuario(9L)).thenReturn(Mono.just("Ghast Sabio 113"));

    StepVerifier.create(useCase.registrar(partida(4, 5, 41_000), 9L))
      .assertNext(r -> assertEquals("Ghast Sabio 113", r.alias()))
      .verifyComplete();
    verify(gateway).vincularDispositivo(UID, 9L);
    verify(gateway).guardarPartida(eq(new Identidad(UID, 9L, "Ghast Sabio 113")), anyString(), anyString(),
      anyInt(), anyInt(), anyInt(), anyLong(), any());
  }

  @Test
  void usuarioNuevoRecibeUnAliasPropioNoElDelDispositivo() {
    when(gateway.buscarJugador(UID)).thenReturn(Mono.just(new Jugador(UID, null, "Zorro Leal 111")));

    StepVerifier.create(useCase.registrar(partida(4, 5, 41_000), 9L))
      .assertNext(r -> assertNotEquals("Zorro Leal 111", r.alias()))
      .verifyComplete();
    verify(gateway).crearUsuario(eq(9L), anyString(), eq(AHORA));
  }

  @Test
  void dispositivoCompartidoOtroUsuarioNoSumaAlPrimero() {
    // El dispositivo es de Ana (usuario 9); juega Beto (usuario 12) en él.
    when(gateway.buscarJugador(UID)).thenReturn(Mono.just(new Jugador(UID, 9L, "Zorro Leal 111")));
    when(gateway.aliasDeUsuario(12L)).thenReturn(Mono.just("Panda Feliz 300"));

    StepVerifier.create(useCase.registrar(partida(4, 5, 41_000), 12L))
      .assertNext(r -> assertEquals("Panda Feliz 300", r.alias()))
      .verifyComplete();
    verify(gateway).guardarPartida(eq(new Identidad(UID, 12L, "Panda Feliz 300")), anyString(), anyString(),
      anyInt(), anyInt(), anyInt(), anyLong(), any());
    verify(gateway, never()).vincularDispositivo(any(), any());
  }

  @Test
  void dispositivoCompartidoSinSesionNoSumaAlUsuarioVinculado() {
    when(gateway.buscarJugador(UID)).thenReturn(Mono.just(new Jugador(UID, 9L, "Zorro Leal 111")));

    StepVerifier.create(useCase.registrar(partida(4, 5, 41_000), null))
      .assertNext(r -> assertEquals("Zorro Leal 111", r.alias()))
      .verifyComplete();
    verify(gateway).guardarPartida(argThat(i -> i.usuarioId() == null && "j".concat(ID).equals(i.clave())),
      anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyLong(), any());
    verify(gateway, never()).aliasDeUsuario(any());
  }

  @Test
  void aliasUsaLasListasFijas() {
    for (int i = 0; i < 50; i++) {
      String[] partes = useCase.nuevoAlias().split(" ");
      assertTrue(TapsRankingUseCase.SUSTANTIVOS.contains(partes[0]));
      assertTrue(TapsRankingUseCase.ADJETIVOS.contains(partes[1]));
      int n = Integer.parseInt(partes[2]);
      assertTrue(n >= 100 && n <= 999);
    }
  }

  // ── Validaciones ───────────────────────────────────────────────────────────

  @Test
  void rechazaPartidasInvalidas() {
    Partida[] invalidas = {
      new Partida("no-es-uuid", "mc-gancho", "minecraft", 4, 5, 41_000L),
      new Partida(ID, "MC Gancho", "minecraft", 4, 5, 41_000L),
      new Partida(ID, "a".repeat(61), "minecraft", 4, 5, 41_000L),
      new Partida(ID, "mc-gancho", "historia", 4, 5, 41_000L),
      new Partida(ID, "mc-gancho", "minecraft", 6, 5, 41_000L),
      new Partida(ID, "mc-gancho", "minecraft", -1, 5, 41_000L),
      new Partida(ID, "mc-gancho", "minecraft", 0, 0, 41_000L),
      new Partida(ID, "mc-gancho", "minecraft", 4, 11, 41_000L),
      new Partida(ID, "mc-gancho", "minecraft", 4, 5, null),
      new Partida(null, "mc-gancho", "minecraft", 4, 5, 41_000L),
      // Fuera de la lista blanca, otro tema u otro total que el del Tap.
      new Partida(ID, "mc-inventado", "minecraft", 4, 5, 41_000L),
      new Partida(ID, "mc-gancho", "logica", 4, 5, 41_000L),
      new Partida(ID, "mc-gancho", "minecraft", 4, 10, 41_000L),
      new Partida(ID, "lg-corto", "logica", 3, 5, 41_000L),
    };
    for (Partida p : invalidas) {
      StepVerifier.create(useCase.registrar(p, null)).expectError(IllegalArgumentException.class).verify();
    }
    noSeGuardoNada();
  }

  @Test
  void aceptaElTotalPropioDeCadaTap() {
    StepVerifier.create(useCase.registrar(new Partida(ID, "lg-corto", "logica", 3, 3, 9_000L), null))
      .assertNext(r -> assertEquals(50, r.xpGanado()))
      .verifyComplete();
  }

  @Test
  void elBancoRealTieneLosTapsDeLaWeb() {
    TapsBanco real = TapsBanco.delClasspath();
    assertEquals(122, real.cantidad());
    assertEquals(new TapsBanco.Tap("cu-plantas-2", "cultura", 5), real.buscar("cu-plantas-2").orElseThrow());
    assertEquals(new TapsBanco.Tap("cu-duelo-2", "cultura", 5), real.buscar("cu-duelo-2").orElseThrow());
    assertEquals(new TapsBanco.Tap("tc-gancho", "tecnologia", 5), real.buscar("tc-gancho").orElseThrow());
    assertEquals(new TapsBanco.Tap("tc-hw-3", "tecnologia", 5), real.buscar("tc-hw-3").orElseThrow());
    assertTrue(TapsRankingUseCase.TEMAS.contains("tecnologia"));
    assertEquals(new TapsBanco.Tap("mc-gancho", "minecraft", 5), real.buscar("mc-gancho").orElseThrow());
    assertTrue(real.buscar("no-existe").isEmpty());
  }

  // ── Límite de frecuencia ───────────────────────────────────────────────────

  @Test
  void sinTurnoResponde429YNoGuarda() {
    when(gateway.reservarTurno(eq(UID), eq(AHORA), eq(java.time.Duration.ofSeconds(10)), eq(60))).thenReturn(Mono.just(false));

    StepVerifier.create(useCase.registrar(partida(4, 5, 41_000), null)).expectError(TapsLimiteException.class).verify();
    noSeGuardoNada();
  }

  // ── Ranking ────────────────────────────────────────────────────────────────

  @Test
  void rankingDeLaSemanaMarcaAlJugadorYDaSuPercentil() {
    Identidad yo = new Identidad(UID, null, "Creeper Veloz 482");
    when(gateway.buscarJugador(UID)).thenReturn(Mono.just(new Jugador(UID, null, "Creeper Veloz 482")));
    when(gateway.top(Instant.parse("2026-10-19T05:00:00Z"), Instant.parse("2026-10-26T05:00:00Z"), null, 20))
      .thenReturn(Flux.just(new Fila(1, "Ghast Sabio 113", 900, "u4", 120), new Fila(2, "Creeper Veloz 482", 340, yo.clave(), 120)));
    when(gateway.posicion(eq(yo), any(), any(), isNull())).thenReturn(Mono.just(new Posicion(2, 340, 120, 118, "Creeper Veloz 482")));

    StepVerifier.create(useCase.ranking(null, null, ID, null, null))
      .assertNext(r -> {
        assertEquals("semana", r.periodo());
        assertEquals("2026-10-19T05:00:00Z", r.desde());
        assertEquals("2026-10-26T05:00:00Z", r.hasta());
        assertEquals(120, r.jugadores());
        assertFalse(r.top().get(0).esTu());
        assertTrue(r.top().get(1).esTu());
        assertEquals(new TapsRankingUseCase.Yo(2, 340, 99, "Creeper Veloz 482"), r.yo());
      })
      .verifyComplete();
  }

  @Test
  void rankingConSesionMarcaAlUsuario() {
    when(gateway.aliasDeUsuario(4L)).thenReturn(Mono.just("Ghast Sabio 113"));
    when(gateway.top(any(), any(), isNull(), eq(20))).thenReturn(Flux.just(new Fila(1, "Ghast Sabio 113", 900, "u4", 2)));
    when(gateway.posicion(argThat(i -> "u4".equals(i.clave())), any(), any(), isNull()))
      .thenReturn(Mono.just(new Posicion(1, 900, 2, 1, "Ghast Sabio 113")));

    StepVerifier.create(useCase.ranking("semana", null, ID, null, 4L))
      .assertNext(r -> {
        assertTrue(r.top().get(0).esTu());
        assertEquals(100, r.yo().percentil());
      })
      .verifyComplete();
  }

  @Test
  void rankingDeHoyPorTemaSinJugadorNoTieneYoYLimitaA50() {
    when(gateway.top(eq(Instant.parse("2026-10-21T05:00:00Z")), any(), eq("logica"), eq(50))).thenReturn(Flux.empty());

    StepVerifier.create(useCase.ranking("hoy", "logica", null, 500, null))
      .assertNext(r -> {
        assertEquals("hoy", r.periodo());
        assertEquals(0, r.jugadores());
        assertTrue(r.top().isEmpty());
        assertNull(r.yo());
      })
      .verifyComplete();
  }

  @Test
  void rankingRechazaParametrosInvalidos() {
    StepVerifier.create(useCase.ranking("mes", null, null, null, null)).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(useCase.ranking("semana", "historia", null, null, null)).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(useCase.ranking("semana", null, "123", null, null)).expectError(IllegalArgumentException.class).verify();
  }
}
