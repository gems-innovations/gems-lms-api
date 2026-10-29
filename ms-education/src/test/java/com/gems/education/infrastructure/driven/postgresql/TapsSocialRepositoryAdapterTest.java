package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.TapsBanco;
import com.gems.education.application.TapsRankingUseCase;
import com.gems.education.application.TapsRegistrados;
import com.gems.education.application.TapsSocialUseCase;
import com.gems.education.application.TapsSocialUseCase.Nuevo;
import com.gems.education.application.exceptions.TapsLimiteException;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** Likes, comentarios, reportes y límite de frecuencia contra un Postgres real (se salta sin Docker). */
@Testcontainers(disabledWithoutDocker = true)
class TapsSocialRepositoryAdapterTest {
  @Container
  private static final GenericContainer<?> POSTGRES = new GenericContainer<>("postgres:16")
    .withEnv("POSTGRES_USER", "prueba").withEnv("POSTGRES_PASSWORD", "prueba").withEnv("POSTGRES_DB", "prueba")
    .withExposedPorts(5432)
    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

  private static final Instant T0 = Instant.parse("2026-10-29T20:00:00Z");
  private static final String TAP = "mc-gancho";
  private static final String DISPOSITIVO = "3f1c2b7a-9d4e-4c1a-8b2f-0a1b2c3d4e5f";

  private static DatabaseClient db;
  private static TapsSocialRepositoryAdapter adapter;
  private static TapsRepositoryAdapter taps;

  @BeforeAll
  static void esquema() {
    var cf = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
      .host(POSTGRES.getHost()).port(POSTGRES.getMappedPort(5432))
      .username("prueba").password("prueba").database("prueba").build());
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(cf).block();
    new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).populate(cf).block();
    db = DatabaseClient.create(cf);
    adapter = new TapsSocialRepositoryAdapter(db);
    taps = new TapsRepositoryAdapter(db);
  }

  @BeforeEach
  void limpiar() {
    db.sql("TRUNCATE taps_like, taps_comentario, taps_comentario_reporte, taps_usuario").then().block();
  }

  private static TapsSocialUseCase en(Instant ahora) {
    Clock reloj = Clock.fixed(ahora, ZoneOffset.UTC);
    TapsBanco banco = TapsBanco.delClasspath();
    return new TapsSocialUseCase(adapter, TapsRegistrados.deBanco(banco),
      new TapsRankingUseCase(taps, banco, reloj, new Random(1)), reloj);
  }

  @Test
  void empiezaEnCeroYElLikeEsIdempotente() {
    var s = en(T0);
    assertEquals(new TapsSocialUseCase.Social(0, 0, false), s.social(TAP, null, DISPOSITIVO).block());
    s.like(TAP, true, null, DISPOSITIVO).block();
    assertEquals(new TapsSocialUseCase.Social(1, 0, true), s.like(TAP, true, null, DISPOSITIVO).block());
    // El usuario con sesión es otra identidad: no ve el like del dispositivo como suyo.
    assertEquals(new TapsSocialUseCase.Social(2, 0, true), s.like(TAP, true, 9L, DISPOSITIVO).block());
    assertEquals(new TapsSocialUseCase.Social(1, 0, false), s.like(TAP, false, null, DISPOSITIVO).block());
    assertEquals(new TapsSocialUseCase.Social(1, 0, false), s.like(TAP, false, null, DISPOSITIVO).block());
    assertThrows(IllegalArgumentException.class, () -> s.like("no-existe", true, 9L, null).block());
  }

  @Test
  void comentarPaginarYBorrarSoloElPropio() {
    for (int i = 0; i < 3; i++) en(T0.plusSeconds(20L * i)).comentar(TAP, new Nuevo("hola " + i), 9L).block();
    en(T0.plusSeconds(60)).comentar(TAP, new Nuevo("de otra persona"), 10L).block();

    var p1 = en(T0).comentarios(TAP, null, 2, 9L).block();
    assertEquals(List.of("de otra persona", "hola 2"), p1.comentarios().stream().map(TapsSocialUseCase.Comentario::texto).toList());
    assertEquals(List.of(false, true), p1.comentarios().stream().map(TapsSocialUseCase.Comentario::mio).toList());
    var p2 = en(T0).comentarios(TAP, p1.siguiente(), 2, 9L).block();
    assertEquals(List.of("hola 1", "hola 0"), p2.comentarios().stream().map(TapsSocialUseCase.Comentario::texto).toList());
    assertNull(p2.siguiente());
    // El alias es el de Taps, nunca el nombre.
    assertNotNull(p1.comentarios().getFirst().alias());

    long ajeno = p1.comentarios().getFirst().id();
    assertFalse(en(T0).borrar(TAP, ajeno, 9L).block());
    assertTrue(en(T0).borrar(TAP, p1.comentarios().get(1).id(), 9L).block());
    assertEquals(3, en(T0).social(TAP, null, null).block().comentarios());
  }

  @Test
  void tresReportesDistintosOcultan() {
    long id = en(T0).comentar(TAP, new Nuevo("comentario"), 9L).block().id();
    en(T0).reportar(TAP, id, 20L).block();
    en(T0).reportar(TAP, id, 20L).block();
    en(T0).reportar(TAP, id, 21L).block();
    assertEquals(1, en(T0).social(TAP, null, null).block().comentarios());
    en(T0).reportar(TAP, id, 22L).block();
    assertEquals(0, en(T0).social(TAP, null, null).block().comentarios());
    assertTrue(en(T0).comentarios(TAP, null, null, null).block().comentarios().isEmpty());
    // Reportar algo que no existe no falla.
    en(T0).reportar(TAP, 999_999L, 20L).block();
  }

  @Test
  void limiteDeFrecuencia() {
    en(T0).comentar(TAP, new Nuevo("uno"), 9L).block();
    assertThrows(TapsLimiteException.class, () -> en(T0.plusSeconds(5)).comentar(TAP, new Nuevo("dos"), 9L).block());
    for (int i = 1; i < 10; i++) en(T0.plusSeconds(16L * i)).comentar(TAP, new Nuevo("n" + i), 9L).block();
    assertThrows(TapsLimiteException.class, () -> en(T0.plusSeconds(16L * 11)).comentar(TAP, new Nuevo("once"), 9L).block());
    // Pasada la hora se puede otra vez.
    en(T0.plusSeconds(3601)).comentar(TAP, new Nuevo("de nuevo"), 9L).block();
  }
}
