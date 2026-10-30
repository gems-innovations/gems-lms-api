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
    return new Tap(id, owner, inst, "Roma", "private", "borrador", 0, 0, JSON, null, null, creado, creado, null);
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
  void publicarDespublicarYLoPublicoSoloConEnlaceYEnElAire() {
    adapter.crear(nuevo("u-bcdfghjkmn", 8L, null, T0)).block();
    assertNull(adapter.buscarPublicado("u-bcdfghjkmn").block(), "un borrador no es público");

    assertNull(adapter.publicar("u-bcdfghjkmn", 3L, null, "enlace", JSON, T0.plusSeconds(1)).block(), "otro dueño no publica");
    assertNull(adapter.publicar("u-bcdfghjkmn", 8L, 5, "enlace", JSON, T0.plusSeconds(1)).block(), "revisión vieja: nada");
    Tap p = adapter.publicar("u-bcdfghjkmn", 8L, 0, "enlace", JSON, T0.plusSeconds(1)).block();
    assertEquals("publicado", p.estado());
    assertEquals("enlace", p.visibilidad());
    assertEquals(1, p.version());
    assertEquals(1, p.revision());
    assertEquals(JSON, p.publicado());
    assertEquals(T0.plusSeconds(1), p.publicadoEn());
    var publico = adapter.buscarPublicado("u-bcdfghjkmn").block();
    assertEquals(1, publico.version());
    assertEquals(JSON, publico.publicado());

    // Editar no lo saca del aire; publicar privado sí (visibilidad), despublicar también (estado).
    adapter.actualizar("u-bcdfghjkmn", 8L, null, "Roma II", "{\"v\":2}", T0.plusSeconds(2)).block();
    assertEquals(JSON, adapter.buscarPublicado("u-bcdfghjkmn").block().publicado(), "sigue la copia congelada");
    Tap priv = adapter.publicar("u-bcdfghjkmn", 8L, null, "private", "{\"v\":2}", T0.plusSeconds(3)).block();
    assertEquals(2, priv.version());
    assertNull(adapter.buscarPublicado("u-bcdfghjkmn").block(), "privado no es público");
    adapter.publicar("u-bcdfghjkmn", 8L, null, "enlace", "{\"v\":2}", T0.plusSeconds(4)).block();
    assertNotNull(adapter.buscarPublicado("u-bcdfghjkmn").block());
    assertNull(adapter.despublicar("u-bcdfghjkmn", 3L, T0.plusSeconds(5)).block(), "otro dueño no retira");
    Tap d = adapter.despublicar("u-bcdfghjkmn", 8L, T0.plusSeconds(5)).block();
    assertEquals("borrador", d.estado());
    assertEquals("private", d.visibilidad());
    assertEquals("{\"v\":2}", d.publicado(), "la copia se conserva hasta la siguiente publicación");
    assertNull(adapter.buscarPublicado("u-bcdfghjkmn").block(), "retirado: deja de ser público de inmediato");
    assertNull(adapter.despublicar("u-bcdfghjkmn", 8L, T0.plusSeconds(6)).block(), "ya era borrador");

    adapter.borrar("u-bcdfghjkmn", 8L, T0.plusSeconds(7)).block();
    assertNull(adapter.publicar("u-bcdfghjkmn", 8L, null, "enlace", JSON, T0.plusSeconds(8)).block(), "un borrado no se publica");
  }

  @Test
  void revisionEnviarAprobarRechazarYLaCarreraConElBorrador() {
    adapter.crear(nuevo("u-bcdfghjkmn", 8L, null, T0)).block();
    adapter.crear(nuevo("u-pqrstvwxz2", 9L, null, T0)).block();
    assertEquals(0, adapter.listarEnRevision().count().block());

    assertNull(adapter.enviarRevision("u-bcdfghjkmn", 3L, null, T0.plusSeconds(1)).block(), "otro dueño no envía");
    Tap e = adapter.enviarRevision("u-bcdfghjkmn", 8L, 0, T0.plusSeconds(1)).block();
    assertEquals("en_revision", e.estado());
    assertEquals("enlace", e.visibilidad());
    assertNull(adapter.enviarRevision("u-bcdfghjkmn", 8L, null, T0.plusSeconds(2)).block(), "ya está en revisión");
    assertNull(adapter.publicar("u-bcdfghjkmn", 8L, null, "private", JSON, T0.plusSeconds(2)).block(), "en revisión no se publica directo");
    assertNull(adapter.buscarPublicado("u-bcdfghjkmn").block(), "en revisión sin versión aprobada no es público");
    assertEquals(List.of("u-bcdfghjkmn"), adapter.listarEnRevision().map(Tap::id).collectList().block());
    assertNotNull(adapter.buscarEnRevision("u-bcdfghjkmn").block());
    assertNull(adapter.buscarEnRevision("u-pqrstvwxz2").block(), "un borrador no está en la cola");

    // El creador edita mientras tanto: la revisión que vio el revisor ya no vale.
    adapter.actualizar("u-bcdfghjkmn", 8L, null, "Roma II", "{\"v\":2}", T0.plusSeconds(3)).block();
    assertNull(adapter.aprobar("u-bcdfghjkmn", 1, "{\"v\":2}", 1L, T0.plusSeconds(4)).block(), "revisión distinta: nada");
    Tap a = adapter.aprobar("u-bcdfghjkmn", 2, "{\"v\":2}", 1L, T0.plusSeconds(4)).block();
    assertEquals("publicado", a.estado());
    assertEquals(1, a.version());
    assertEquals("{\"v\":2}", a.publicado());
    assertEquals("{\"v\":2}", adapter.buscarPublicado("u-bcdfghjkmn").block().publicado());
    assertNull(adapter.aprobar("u-bcdfghjkmn", 3, "{\"v\":2}", 1L, T0.plusSeconds(5)).block(), "ya no está en revisión");
    Long revisor = db.sql("SELECT revisado_por FROM taps_creador WHERE id = 'u-bcdfghjkmn'").map(r -> r.get("revisado_por", Long.class)).one().block();
    assertEquals(1L, revisor);

    // Segunda versión a revisión (re-revisión): la aprobada sigue en el aire; pedir cambios/rechazar guarda el motivo
    // pero NO la tumba (decisión del dueño, 27-oct): sigue publicada hasta nueva decisión.
    adapter.enviarRevision("u-bcdfghjkmn", 8L, null, T0.plusSeconds(6)).block();
    assertEquals("{\"v\":2}", adapter.buscarPublicado("u-bcdfghjkmn").block().publicado(), "la versión aprobada sigue mientras se revisa la nueva");
    Tap enCola = adapter.buscarEnRevision("u-bcdfghjkmn").block();
    Tap r = adapter.rechazar("u-bcdfghjkmn", enCola.revision(), "Dos respuestas correctas", 1L, T0.plusSeconds(7)).block();
    assertEquals("rechazado", r.estado());
    assertEquals("enlace", r.visibilidad(), "con copia aprobada previa no pasa a privado");
    assertEquals("Dos respuestas correctas", r.motivo());
    assertEquals("{\"v\":2}", r.publicado());
    var sigue = adapter.buscarPublicado("u-bcdfghjkmn").block();
    assertNotNull(sigue, "re-revisión rechazada: la copia aprobada sigue publicada");
    assertEquals("{\"v\":2}", sigue.publicado());
    assertEquals(1, sigue.version());
    assertEquals(0, adapter.listarEnRevision().count().block());
    // Desde rechazado se puede volver a enviar y el motivo se limpia; la copia aprobada (con enlace) sigue en el aire.
    Tap otraVez = adapter.enviarRevision("u-bcdfghjkmn", 8L, null, T0.plusSeconds(8)).block();
    assertEquals("en_revision", otraVez.estado());
    assertNull(otraVez.motivo());
    assertEquals("{\"v\":2}", otraVez.publicado());
    assertEquals("{\"v\":2}", adapter.buscarPublicado("u-bcdfghjkmn").block().publicado());
    // Despublicar explícito del dueño sí la retira aunque esté en revisión.
    Tap retirado = adapter.despublicar("u-bcdfghjkmn", 8L, T0.plusSeconds(9)).block();
    assertEquals("private", retirado.visibilidad());
    assertNull(adapter.buscarPublicado("u-bcdfghjkmn").block(), "despublicar: deja de ser público de inmediato");

    // Primera revisión rechazada (sin copia previa): pasa a privado y no hay nada público.
    adapter.crear(nuevo("u-3456789bcd", 8L, null, T0)).block();
    Tap primera = adapter.enviarRevision("u-3456789bcd", 8L, null, T0.plusSeconds(10)).block();
    Tap r1 = adapter.rechazar("u-3456789bcd", primera.revision(), "Falta la explicación", 1L, T0.plusSeconds(11)).block();
    assertEquals("rechazado", r1.estado());
    assertEquals("private", r1.visibilidad());
    assertNull(r1.publicado());
    assertNull(adapter.buscarPublicado("u-3456789bcd").block(), "primera revisión rechazada: nada público");
    assertNull(adapter.enviarRevision("u-3456789bcd", 8L, null, T0.plusSeconds(12)).block().publicado(), "sin copia no aparece ninguna");
    assertNull(adapter.buscarPublicado("u-3456789bcd").block());

    // Una copia publicada en privado (sin revisar) no se vuelve pública por pedir el enlace.
    adapter.publicar("u-pqrstvwxz2", 9L, null, "private", "{\"v\":9}", T0.plusSeconds(9)).block();
    Tap pedida = adapter.enviarRevision("u-pqrstvwxz2", 9L, null, T0.plusSeconds(10)).block();
    assertEquals("en_revision", pedida.estado());
    assertNull(pedida.publicado(), "la copia privada se anula");
    assertNull(adapter.buscarPublicado("u-pqrstvwxz2").block(), "en revisión sin versión aprobada: nada público");
  }

  @Test
  void importarElMismoBorradorDosVecesNoDuplicaYElOrigenEsPorDueno() {
    Tap primero = adapter.crearImportado(nuevo("u-bcdfghjkmn", 8L, null, T0), "mh3k2a9f-0abc123").block();
    assertNotNull(primero);
    // Mismo dueño y mismo origen con otro id: el índice único parcial lo frena y no hay fila nueva.
    assertNull(adapter.crearImportado(nuevo("u-pqrstvwxz2", 8L, null, T0), "mh3k2a9f-0abc123").block());
    assertEquals(1L, adapter.contarVivos(8L).block());
    assertEquals("u-bcdfghjkmn", adapter.buscarPorOrigen(8L, "mh3k2a9f-0abc123").block().id());
    // Otro dueño con el mismo borradorId no ve el ajeno y sí puede tener el suyo.
    assertNull(adapter.buscarPorOrigen(9L, "mh3k2a9f-0abc123").block());
    assertNotNull(adapter.crearImportado(nuevo("u-pqrstvwxz2", 9L, "inst-1", T0), "mh3k2a9f-0abc123").block());
    // Un id repetido tampoco inserta nada (misma señal: vacío).
    assertNull(adapter.crearImportado(nuevo("u-bcdfghjkmn", 10L, null, T0), "otro-1234567").block());
    // Tras borrar el importado, el origen queda libre y se puede importar otra vez.
    assertTrue(adapter.borrar("u-bcdfghjkmn", 8L, T0.plusSeconds(1)).block());
    assertNull(adapter.buscarPorOrigen(8L, "mh3k2a9f-0abc123").block());
    assertNotNull(adapter.crearImportado(nuevo("u-23456789bc", 8L, null, T0.plusSeconds(2)), "mh3k2a9f-0abc123").block());
    // Los Taps normales no llevan origen y no chocan entre sí.
    assertNotNull(adapter.crear(nuevo("u-cdfghjkmnp", 8L, null, T0)).block());
    assertNotNull(adapter.crear(nuevo("u-dfghjkmnpq", 8L, null, T0)).block());
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
