package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.TapsBanco;
import com.gems.education.application.TapsEstadisticasUseCase;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** El adaptador SQL de las estadísticas de Taps contra un Postgres 16 real (se omite si no hay Docker). */
@Testcontainers(disabledWithoutDocker = true)
class TapsEstadisticasRepositoryAdapterTest {
  @Container
  private static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:16")
    .withEnv("POSTGRES_USER", "prueba").withEnv("POSTGRES_PASSWORD", "prueba").withEnv("POSTGRES_DB", "prueba")
    .withExposedPorts(5432)
    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

  private static DatabaseClient db;
  private static TapsEstadisticasRepositoryAdapter adapter;

  @BeforeAll
  static void esquema() {
    var cf = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
      .host(POSTGRES.getHost()).port(POSTGRES.getMappedPort(5432))
      .username("prueba").password("prueba").database("prueba").build());
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(cf).block();
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(cf).block();   // idempotente
    db = DatabaseClient.create(cf);
    adapter = new TapsEstadisticasRepositoryAdapter(db);
  }

  @BeforeEach
  void limpiar() {
    db.sql("TRUNCATE taps_estadistica").then().block();
  }

  @Test
  void sumaAtomicaYLecturaPorRango() {
    LocalDate d1 = LocalDate.parse("2026-10-22"), d2 = LocalDate.parse("2026-10-24");
    Flux.range(0, 40).flatMap(i -> adapter.sumar("mc-gancho", d2, "vista", 0), 8).then().block();   // en paralelo: no se pierde ninguna
    adapter.sumar("mc-gancho", d1, "vista", 0).block();
    adapter.sumar("mc-gancho", d2, "respuesta", 3).block();
    adapter.sumar("fleming", d1, "completado", 0).block();

    var todo = adapter.leer(d1, d2).collectList().block();
    assertEquals(41, todo.stream().filter(f -> f.tapId().equals("mc-gancho") && f.evento().equals("vista")).findFirst().orElseThrow().cantidad());
    assertEquals(3, todo.stream().filter(f -> f.evento().equals("respuesta")).findFirst().orElseThrow().pregunta());
    assertEquals(3, todo.size());

    var soloD2 = adapter.leer(d2, d2).collectList().block();
    assertEquals(40, soloD2.stream().filter(f -> f.evento().equals("vista")).findFirst().orElseThrow().cantidad());
    assertTrue(soloD2.stream().noneMatch(f -> f.tapId().equals("fleming")));
  }

  @Test
  void elCasoDeUsoFuncionaDeExtremoAExtremo() {
    var caso = new TapsEstadisticasUseCase(adapter, TapsBanco.delClasspath(), Clock.fixed(Instant.parse("2026-10-24T15:00:00Z"), ZoneOffset.UTC));
    caso.registrar(new TapsEstadisticasUseCase.Evento("mc-gancho", "vista", null, null)).block();
    caso.registrar(new TapsEstadisticasUseCase.Evento("mc-gancho", "respuesta", 0, true)).block();
    caso.registrar(new TapsEstadisticasUseCase.Evento("mc-gancho", "completado", null, null)).block();
    var r = caso.leer(7).block();
    assertEquals("mc-gancho", r.masVisto());
    var t = r.taps().getFirst();
    assertEquals(1, t.vistas());
    assertEquals(1, t.primeraRespuesta());
    assertEquals(1, t.aciertos());
    assertEquals(1, t.completados());
  }
}