package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.TapsBanco;
import com.gems.education.application.TapsRankingUseCase;
import com.gems.education.application.TapsRankingUseCase.Partida;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsGateway.Identidad;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El adaptador SQL de Taps contra un Postgres 16 real (Testcontainers). Se omite si no hay Docker; en GitHub
 * Actions (ubuntu-latest) sí lo hay.
 */
@Testcontainers(disabledWithoutDocker = true)
class TapsRepositoryAdapterTest {
  @Container
  private static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:16")
    .withEnv("POSTGRES_USER", "prueba").withEnv("POSTGRES_PASSWORD", "prueba").withEnv("POSTGRES_DB", "prueba")
    .withExposedPorts(5432)
    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

  /** Miércoles 21 de octubre de 2026, 15:00 en Bogotá. */
  private static final Instant T0 = Instant.parse("2026-10-21T20:00:00Z");

  private static DatabaseClient db;
  private static TapsRepositoryAdapter adapter;
  private final TapsBanco banco = TapsBanco.delClasspath();

  @BeforeAll
  static void esquema() {
    var cf = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
      .host(POSTGRES.getHost()).port(POSTGRES.getMappedPort(5432))
      .username("prueba").password("prueba").database("prueba").build());
    // Dos veces: el esquema se aplica en cada arranque y debe ser idempotente.
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(cf).block();
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(cf).block();
    db = DatabaseClient.create(cf);
    adapter = new TapsRepositoryAdapter(db);
  }

  @BeforeEach
  void limpiar() {
    db.sql("TRUNCATE taps_partida, taps_jugador, taps_usuario").then().block();
  }

  private TapsRankingUseCase en(Instant ahora) {
    return new TapsRankingUseCase(adapter, banco, Clock.fixed(ahora, ZoneOffset.UTC), new Random(ahora.getEpochSecond()));
  }

  private static Partida partida(String jugador, String tap, String tema, int aciertos) {
    return new Partida(jugador, tap, tema, aciertos, 5, 41_000L);
  }

  @Test
  void repeticionSumaYPuestos() {
    String a = UUID.randomUUID().toString();
    int[] xp = new int[5];
    for (int i = 0; i < 5; i++) {
      xp[i] = en(T0.plusSeconds(11L * i)).registrar(partida(a, "mc-gancho", "minecraft", 5), null).block().xpGanado();
    }
    assertArrayEquals(new int[] {70, 70, 70, 17, 17}, xp);

    var r = en(T0.plusSeconds(100)).ranking("semana", null, a, null, null).block();
    assertEquals(1, r.jugadores());
    assertEquals(244, r.yo().xp());
    assertEquals(100, r.yo().percentil());
    assertTrue(r.top().getFirst().esTu());
  }

  @Test
  void empateLoGanaQuienLlegoAntes() {
    String tarde = UUID.randomUUID().toString();
    String temprano = UUID.randomUUID().toString();
    String tercero = UUID.randomUUID().toString();
    en(T0).registrar(partida(temprano, "mc-gancho", "minecraft", 4), null).block();
    en(T0.plusSeconds(1)).registrar(partida(tercero, "mc-gancho", "minecraft", 1), null).block();
    // temprano llegó a 40 XP en T0 y tarde en T0+2: con el mismo XP va primero temprano.
    en(T0.plusSeconds(2)).registrar(partida(tarde, "mc-gancho", "minecraft", 4), null).block();

    var r = en(T0.plusSeconds(60)).ranking("hoy", null, tarde, null, null).block();
    assertEquals(List.of(40, 40, 10), r.top().stream().map(TapsRankingUseCase.Entrada::xp).toList());
    assertFalse(r.top().get(0).esTu());
    assertTrue(r.top().get(1).esTu());
    // Supera solo a 1 de los otros 2.
    assertEquals(new TapsRankingUseCase.Yo(2, 40, 50, r.top().get(1).alias()), r.yo());
  }

  @Test
  void dispositivoCompartidoNoMezclaPersonas() {
    String d = UUID.randomUUID().toString();
    var ana = en(T0).registrar(partida(d, "mc-gancho", "minecraft", 5), 9L).block();
    var anonima = en(T0.plusSeconds(20)).registrar(partida(d, "mc-gancho", "minecraft", 4), null).block();
    var beto = en(T0.plusSeconds(40)).registrar(partida(d, "lg-gancho", "logica", 2), 12L).block();

    var r = en(T0.plusSeconds(60)).ranking("semana", null, null, null, null).block();
    assertEquals(3, r.jugadores());
    assertEquals(List.of(70, 40, 20), r.top().stream().map(TapsRankingUseCase.Entrada::xp).toList());
    assertEquals(List.of(ana.alias(), anonima.alias(), beto.alias()),
      r.top().stream().map(TapsRankingUseCase.Entrada::alias).toList());
    assertEquals(3, r.top().stream().map(TapsRankingUseCase.Entrada::alias).distinct().count());

    // Ana desde otro dispositivo suma a la misma identidad y conserva su alias.
    var otra = en(T0.plusSeconds(80)).registrar(partida(UUID.randomUUID().toString(), "mc-gancho", "minecraft", 3), 9L).block();
    assertEquals(ana.alias(), otra.alias());
    assertEquals(100, otra.semana().xp());
    // Por tema: solo Beto jugó lógica.
    var logica = en(T0.plusSeconds(90)).ranking("semana", "logica", d, null, 12L).block();
    assertEquals(1, logica.jugadores());
    assertTrue(logica.top().getFirst().esTu());
  }

  @Test
  void turnoAtomicoConEnviosSimultaneos() {
    UUID d = UUID.randomUUID();
    en(T0).registrar(partida(d.toString(), "mc-gancho", "minecraft", 1), null).block();
    Instant despues = T0.plusSeconds(30);

    List<Boolean> turnos = Flux.range(0, 12)
      .flatMap(i -> adapter.reservarTurno(d, despues, Duration.ofSeconds(10), 60).subscribeOn(Schedulers.parallel()), 12)
      .collectList().block();
    assertEquals(1, turnos.stream().filter(Boolean::booleanValue).count(), turnos.toString());

    // Por el caso de uso: dos partidas a la vez, una sola entra y la otra es 429.
    Instant otra = T0.plusSeconds(60);
    List<String> resultados = Flux.range(0, 2)
      .flatMap(i -> en(otra).registrar(partida(d.toString(), "mc-gancho", "minecraft", 2), null)
        .map(x -> "ok").onErrorResume(TapsLimiteException.class, e -> reactor.core.publisher.Mono.just("429"))
        .subscribeOn(Schedulers.parallel()))
      .collectList().block();
    assertEquals(1, resultados.stream().filter("ok"::equals).count(), resultados.toString());
  }

  @Test
  void sesentaPorHoraYDiezSegundosEntrePartidas() {
    UUID d = UUID.randomUUID();
    en(T0).registrar(partida(d.toString(), "mc-gancho", "minecraft", 1), null).block();
    assertFalse(adapter.reservarTurno(d, T0.plusSeconds(9), Duration.ofSeconds(10), 60).block());
    for (int i = 1; i < 60; i++) {
      assertTrue(adapter.reservarTurno(d, T0.plusSeconds(10L * i), Duration.ofSeconds(10), 60).block(), "turno " + i);
    }
    assertFalse(adapter.reservarTurno(d, T0.plusSeconds(600), Duration.ofSeconds(10), 60).block());
    // Al vencer la ventana de una hora se puede volver a jugar.
    assertTrue(adapter.reservarTurno(d, T0.plusSeconds(3600), Duration.ofSeconds(10), 60).block());
  }

  @Test
  void repeticionesCuentaPorIdentidad() {
    String d = UUID.randomUUID().toString();
    for (int i = 0; i < 3; i++) {
      en(T0.plusSeconds(11L * i)).registrar(partida(d, "mc-gancho", "minecraft", 5), 9L).block();
    }
    // Las partidas anónimas del mismo dispositivo no cuentan las del usuario.
    var anonima = en(T0.plusSeconds(40)).registrar(partida(d, "mc-gancho", "minecraft", 5), null).block();
    assertEquals(70, anonima.xpGanado());
    assertEquals(3L, adapter.repeticiones(new Identidad(UUID.fromString(d), 9L, "x"), "mc-gancho",
      T0.minusSeconds(60), T0.plusSeconds(600)).block());
  }
}
