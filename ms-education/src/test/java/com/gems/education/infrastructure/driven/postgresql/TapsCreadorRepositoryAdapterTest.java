package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.TapsCreadorGateway.Tap;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** La tabla taps_creador contra un Postgres 16 real (Testcontainers): propiedad en el WHERE, revisión y borrado lógico. */
@Testcontainers(disabledWithoutDocker = true)
class TapsCreadorRepositoryAdapterTest {
  @Container
  private static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:16")
    .withEnv("POSTGRES_USER", "prueba").withEnv("POSTGRES_PASSWORD", "prueba").withEnv("POSTGRES_DB", "prueba")
    .withExposedPorts(5432)
    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

  private static final Instant T0 = Instant.parse("2026-10-26T15:00:00Z");
  private static final String JSON = "{\"v\":1,\"theme\":\"epoca\",\"title\":\"Roma\"}";

  private static DatabaseClient db;
  private static TapsCreadorRepositoryAdapter adapter;

  @BeforeAll
  static void esquema() {
    var cf = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
      .host(POSTGRES.getHost()).port(POSTGRES.getMappedPort(5432))
      .username("prueba").password("prueba").database("prueba").build());
    // Dos veces: el esquema se aplica en cada arranque y debe ser idempotente.
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(cf).block();
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(cf).block();
    db = DatabaseClient.create(cf);
    adapter = new TapsCreadorRepositoryAdapter(db);
  }

  @BeforeEach
  void limpiar() {
    db.sql("TRUNCATE taps_creador").then().block();
  }

  private static Tap nuevo(String id, Long owner, String inst, Instant creado) {
    return new Tap(id, owner, inst, "Roma", "private", "borrador", 0, 0, JSON, null, creado, creado, null);
  }

  @Test
  void crearDevuelveLaFilaYUnIdRepetidoNoInsertaNada() {
    Tap t = adapter.crear(nuevo("u-bcdfghjkmn", 8L, null, T0)).block();
    assertNotNull(t);
    assertEquals(8L, t.ownerId());
    assertNull(t.institutionId());
    assertEquals(0, t.revision());
    assertEquals(T0, t.creado());
    assertNull(adapter.crear(nuevo("u-bcdfghjkmn", 9L, "inst-1", T0)).block(), "el choque devuelve vacío");
    assertEquals(8L, adapter.buscar("u-bcdfghjkmn", 8L).block().ownerId(), "la fila original sigue intacta");
  }

  @Test
  void buscarListarActualizarYBorrarSoloVenLoDelDueno() {
    adapter.crear(nuevo("u-bcdfghjkmn", 8L, null, T0)).block();
    adapter.crear(nuevo("u-pqrstvwxz2", 8L, null, T0.plusSeconds(1))).block();
    adapter.crear(nuevo("u-3456789bcd", 3L, "inst-1", T0)).block();

    assertNotNull(adapter.buscar("u-bcdfghjkmn", 8L).block());
    assertNull(adapter.buscar("u-bcdfghjkmn", 3L).block(), "otro dueño no lo ve");
    assertEquals(List.of("u-pqrstvwxz2", "u-bcdfghjkmn"), adapter.listar(8L, null).map(Tap::id).collectList().block(),
      "del más reciente al más antiguo");
    assertEquals(2, adapter.listar(8L, "borrador").count().block());
    assertEquals(0, adapter.listar(8L, "publicado").count().block());
    assertEquals(1, adapter.listar(3L, null).count().block());

    assertNull(adapter.actualizar("u-bcdfghjkmn", 3L, null, "Hack", JSON, T0.plusSeconds(5)).block(), "otro dueño no edita");
    assertEquals("Roma", adapter.buscar("u-bcdfghjkmn", 8L).block().titulo());
    assertFalse(adapter.borrar("u-bcdfghjkmn", 3L, T0.plusSeconds(5)).block(), "otro dueño no borra");
    assertNotNull(adapter.buscar("u-bcdfghjkmn", 8L).block());
  }

  @Test
  void actualizarSubeLaRevisionYRespetaLaEsperada() {
    adapter.crear(nuevo("u-bcdfghjkmn", 8L, null, T0)).block();
    Tap r1 = adapter.actualizar("u-bcdfghjkmn", 8L, 0, "Roma II", JSON, T0.plusSeconds(10)).block();
    assertEquals(1, r1.revision());
    assertEquals("Roma II", r1.titulo());
    assertEquals(T0.plusSeconds(10), r1.actualizado());
    assertEquals(T0, r1.creado());
    assertNull(adapter.actualizar("u-bcdfghjkmn", 8L, 0, "Roma III", JSON, T0.plusSeconds(20)).block(), "revisión vieja: nada");
    assertEquals("Roma II", adapter.buscar("u-bcdfghjkmn", 8L).block().titulo());
    Tap r2 = adapter.actualizar("u-bcdfghjkmn", 8L, null, "Roma IV", JSON, T0.plusSeconds(30)).block();
    assertEquals(2, r2.revision());
  }

  @Test
  void borrarEsLogicoYNoDevuelveCuotaDiaria() {
    adapter.crear(nuevo("u-bcdfghjkmn", 8L, null, T0)).block();
    adapter.crear(nuevo("u-pqrstvwxz2", 8L, null, T0.minus(Duration.ofDays(2)))).block();
    assertEquals(2L, adapter.contarVivos(8L).block());
    assertEquals(1L, adapter.contarCreadosDesde(8L, T0.minus(Duration.ofDays(1))).block());

    assertTrue(adapter.borrar("u-bcdfghjkmn", 8L, T0.plusSeconds(1)).block());
    assertFalse(adapter.borrar("u-bcdfghjkmn", 8L, T0.plusSeconds(2)).block(), "ya borrado");
    assertNull(adapter.buscar("u-bcdfghjkmn", 8L).block());
    assertEquals(1, adapter.listar(8L, null).count().block());
    assertEquals(1L, adapter.contarVivos(8L).block());
    assertEquals(1L, adapter.contarCreadosDesde(8L, T0.minus(Duration.ofDays(1))).block(), "borrar no resta del día");
    assertNull(adapter.actualizar("u-bcdfghjkmn", 8L, null, "Zombi", JSON, T0.plusSeconds(3)).block(), "un borrado no se edita");
    Long fila = db.sql("SELECT COUNT(*) AS n FROM taps_creador WHERE id = 'u-bcdfghjkmn' AND borrado_en IS NOT NULL")
      .map(r -> r.get("n", Long.class)).one().block();
    assertEquals(1L, fila, "la fila sigue, marcada");
  }
}
