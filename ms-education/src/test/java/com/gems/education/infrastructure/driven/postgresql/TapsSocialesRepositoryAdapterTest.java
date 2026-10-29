package com.gems.education.infrastructure.driven.postgresql;

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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** El adaptador SQL de los me gusta de Taps contra un Postgres 16 real (se omite si no hay Docker). */
@Testcontainers(disabledWithoutDocker = true)
class TapsSocialesRepositoryAdapterTest {
  @Container
  private static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:16")
    .withEnv("POSTGRES_USER", "prueba").withEnv("POSTGRES_PASSWORD", "prueba").withEnv("POSTGRES_DB", "prueba")
    .withExposedPorts(5432)
    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

  private static DatabaseClient db;
  private static TapsSocialesRepositoryAdapter adapter;

  @BeforeAll
  static void esquema() {
    var cf = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
      .host(POSTGRES.getHost()).port(POSTGRES.getMappedPort(5432))
      .username("prueba").password("prueba").database("prueba").build());
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(cf).block();
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(cf).block();   // idempotente
    db = DatabaseClient.create(cf);
    adapter = new TapsSocialesRepositoryAdapter(db);
  }

  @BeforeEach
  void limpiar() {
    db.sql("TRUNCATE taps_me_gusta, taps_estadistica").then().block();
  }

  @Test
  void unMeGustaPorJugadorYTapAunqueSeRepita() {
    UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    Flux.range(0, 10).flatMap(i -> adapter.ponerMeGusta("mc-gancho", a), 5).then().block();   // en paralelo: sigue siendo uno
    adapter.ponerMeGusta("mc-gancho", b).block();
    adapter.ponerMeGusta("fleming", a).block();
    assertEquals(Map.of("mc-gancho", 2L, "fleming", 1L), adapter.meGusta(List.of("mc-gancho", "fleming", "otro")).block());
    assertEquals(Set.of("mc-gancho", "fleming"), adapter.meGustaDe(a, List.of("mc-gancho", "fleming")).block());

    adapter.quitarMeGusta("mc-gancho", a).block();
    adapter.quitarMeGusta("mc-gancho", a).block();
    assertEquals(Map.of("mc-gancho", 1L), adapter.meGusta(List.of("mc-gancho")).block());
    assertEquals(Set.of("fleming"), adapter.meGustaDe(a, List.of("mc-gancho", "fleming")).block());
    assertEquals(Map.of(), adapter.meGusta(List.of()).block());
  }

  @Test
  void lasVistasSumanSoloElRango() {
    db.sql("INSERT INTO taps_estadistica (tap_id, dia, evento, pregunta, cantidad) VALUES "
      + "('mc-gancho', '2026-10-24', 'vista', 0, 5), ('mc-gancho', '2026-10-01', 'vista', 0, 2), "
      + "('mc-gancho', '2026-09-01', 'vista', 0, 100), ('mc-gancho', '2026-10-24', 'completado', 0, 9), "
      + "('fleming', '2026-10-24', 'vista', 0, 1)").then().block();
    assertEquals(Map.of("mc-gancho", 7L),
      adapter.vistas(List.of("mc-gancho"), LocalDate.parse("2026-09-25"), LocalDate.parse("2026-10-24")).block());
  }
}
