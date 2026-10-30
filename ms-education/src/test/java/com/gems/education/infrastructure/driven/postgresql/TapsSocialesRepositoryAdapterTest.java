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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** El adaptador SQL de me gusta y comentarios de Taps contra un Postgres 16 real (se omite si no hay Docker). */
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
    db.sql("TRUNCATE taps_me_gusta, taps_estadistica, taps_comentario_reporte, taps_comentario").then().block();
  }

  @Test
  void unMeGustaPorUsuarioYTapAunqueSeRepita() {
    Flux.range(0, 10).flatMap(i -> adapter.ponerMeGusta("mc-gancho", 5L), 5).then().block();   // en paralelo: sigue siendo uno
    adapter.ponerMeGusta("mc-gancho", 6L).block();
    adapter.ponerMeGusta("fleming", 5L).block();
    assertEquals(Map.of("mc-gancho", 2L, "fleming", 1L), adapter.meGusta(List.of("mc-gancho", "fleming", "otro")).block());
    assertEquals(Set.of("mc-gancho", "fleming"), adapter.meGustaDe(5L, List.of("mc-gancho", "fleming")).block());

    adapter.quitarMeGusta("mc-gancho", 5L).block();
    adapter.quitarMeGusta("mc-gancho", 5L).block();
    assertEquals(Map.of("mc-gancho", 1L), adapter.meGusta(List.of("mc-gancho")).block());
    assertEquals(Map.of(), adapter.meGusta(List.of()).block());
  }

  @Test
  void lasVistasSumanSoloElRango() {
    db.sql("INSERT INTO taps_estadistica (tap_id, dia, evento, pregunta, cantidad) VALUES "
      + "('mc-gancho', '2026-10-24', 'vista', 0, 5), ('mc-gancho', '2026-10-01', 'vista', 0, 2), "
      + "('mc-gancho', '2026-09-01', 'vista', 0, 100), ('mc-gancho', '2026-10-24', 'completado', 0, 9)").then().block();
    assertEquals(Map.of("mc-gancho", 7L),
      adapter.vistas(List.of("mc-gancho"), LocalDate.parse("2026-09-25"), LocalDate.parse("2026-10-24")).block());
  }

  @Test
  void comentariosPaginasReportesYBorrado() {
    long a = adapter.comentar("mc-gancho", 5L, "Ana R.", "Primero").block().id();
    long b = adapter.comentar("mc-gancho", 6L, "Luis P.", "Segundo").block().id();
    long c = adapter.comentar("mc-gancho", 5L, "Ana R.", "Tercero").block().id();
    adapter.comentar("fleming", 5L, "Ana R.", "Otro Tap").block();

    assertEquals(List.of(c, b), adapter.pagina("mc-gancho", Long.MAX_VALUE, 2).map(x -> x.id()).collectList().block());
    assertEquals(List.of(a), adapter.pagina("mc-gancho", b, 2).map(x -> x.id()).collectList().block());
    assertEquals(3L, adapter.comentariosDesde(5L, Instant.now().minusSeconds(60)).block());
    assertEquals(Map.of("mc-gancho", 3L, "fleming", 1L), adapter.comentarios(List.of("mc-gancho", "fleming")).block());

    assertTrue(adapter.reportar(b, 1L).block());
    assertFalse(adapter.reportar(b, 1L).block());
    adapter.reportar(b, 2L).block();
    adapter.ocultarSiReportado(b, 3).block();
    assertFalse(adapter.buscarComentario(b).block().oculto());
    adapter.reportar(b, 3L).block();
    adapter.ocultarSiReportado(b, 3).block();
    assertTrue(adapter.buscarComentario(b).block().oculto());
    assertEquals(List.of(c, a), adapter.pagina("mc-gancho", Long.MAX_VALUE, 5).map(x -> x.id()).collectList().block());

    adapter.cambiarOculto(b, false).block();   // el superadmin restaura: los reportes ya no lo ocultan
    adapter.reportar(b, 4L).block();
    adapter.ocultarSiReportado(b, 3).block();
    assertFalse(adapter.buscarComentario(b).block().oculto());

    adapter.borrarComentario(a).block();
    assertNull(adapter.buscarComentario(a).block());
    assertEquals(Map.of("mc-gancho", 2L), adapter.comentarios(List.of("mc-gancho")).block());
    assertEquals(3L, adapter.comentariosDesde(5L, Instant.now().minusSeconds(60)).block());   // los borrados cuentan
  }
}
