package com.gems.education.application;

import com.gems.education.application.exceptions.TapsComentarioException;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.education.application.gateway.TapsSocialesGateway;
import com.gems.education.application.gateway.TapsSocialesGateway.Comentario;
import com.gems.education.application.gateway.UserDirectory;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsComentariosUseCaseTest {
  private static final AuthenticatedUser ANA = new AuthenticatedUser(5L, "STUDENT", "inst-1");
  private static final AuthenticatedUser LUIS = new AuthenticatedUser(6L, "STUDENT", "inst-1");
  private static final AuthenticatedUser ADMIN = new AuthenticatedUser(1L, "SUPER_ADMIN", null);
  private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");
  private final TapsSocialesGateway gateway = mock(TapsSocialesGateway.class);
  private final TapsRegistrados registrados = TapsRegistrados.deBanco(new TapsBanco(Map.of(
    "mc-gancho", new TapsBanco.Tap("mc-gancho", "minecraft", 5))));
  private final UserDirectory users = mock(UserDirectory.class);
  private TapsComentariosUseCase caso;

  @BeforeEach
  void setUp() {
    caso = new TapsComentariosUseCase(gateway, registrados, new TapsPayloadValidador(Set.of("tonto")), users,
      Clock.fixed(AHORA, ZoneOffset.UTC));
    when(users.find(5L)).thenReturn(Mono.just(new UserDirectory.UserProfile(5L, "Ana Ruiz", "inst-1", "Ana", "Ruiz", "ana@correo.com")));
    when(gateway.comentariosDesde(any(), any())).thenReturn(Mono.just(0L));
    when(gateway.comentar(any(), any(), any(), any()))
      .thenAnswer(i -> Mono.just(new Comentario(7, i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3), AHORA, false)));
    when(gateway.buscarComentario(7)).thenReturn(Mono.just(new Comentario(7, "mc-gancho", 5L, "Ana R.", "Hola", AHORA, false)));
    when(gateway.buscarComentario(8)).thenReturn(Mono.empty());
    when(gateway.borrarComentario(anyLong())).thenReturn(Mono.empty());
    when(gateway.ocultarSiReportado(anyLong(), anyInt())).thenReturn(Mono.empty());
    when(gateway.cambiarOculto(anyLong(), anyBoolean())).thenReturn(Mono.empty());
  }

  @Test
  void elAutorEsNombreEInicialDelApellido() {
    assertEquals("Ana R.", caso.autor("Ana María", "ruiz Gómez"));
    assertEquals("Ana", caso.autor(" Ana ", ""));
    assertEquals("Ñoño Á.", caso.autor("Ñoño", "álvarez"));
    assertEquals("Jean-Luc O.", caso.autor("Jean-Luc", "O'Brien"));
    // Lo que no es un nombre (apodos de invitado, parte local de un correo de Google, HTML, palabras bloqueadas) no sale.
    for (String malo : new String[]{null, "", "wa.me/573001234567", "juan.perez2011", "<b>Ana</b>", "Tonto", "A".repeat(31)}) {
      assertEquals("Usuario", caso.autor(malo, "Ruiz"), String.valueOf(malo));
    }
    assertEquals("Ana", caso.autor("Ana", "3001234567"));
  }

  @Test
  void comentaConTextoLimpioYDiceQueEsMio() {
    StepVerifier.create(caso.comentar(ANA, "mc-gancho", "  ¡Me encantó   este!  "))
      .expectNext(new TapsComentariosUseCase.Vista(7, "Ana R.", "¡Me encantó este!", AHORA, true)).verifyComplete();
    verify(gateway).comentar("mc-gancho", 5L, "Ana R.", "¡Me encantó este!");
  }

  @Test
  void elFiltroDeContactoNoRechazaComentariosNormales() {
    for (String bueno : new String[]{"Saqué 4 de 5 en 2 minutos", "La 3 era 1789-1799, ¿no?", "Me gusta.Es muy bueno",
      "¡Lo jugué con mi hermana! 10/10"}) {
      StepVerifier.create(caso.comentar(ANA, "mc-gancho", bueno)).expectNextCount(1).verifyComplete();
    }
  }

  @Test
  void rechazaConMotivoLosTextosQueNoPasanElFiltro() {
    for (String malo : new String[]{null, "", "   ", "a".repeat(201), "mira www.sitio.com", "escríbeme a ana@correo.com",
      "llama al 300 123 4567", "<b>hola</b>", "eres un tonto",
      // Saltos del filtro general que en comentarios no pasan (hay menores).
      "llama al 300 - 123 - 4567", "３００１２３４５６７", "t.me/juanito",
      "búscame en insta juanito", "sígueme @juanito_2010", "pasa tu wsp", "entra a mi server discord.gg/abc", "ｗｗｗ.sitio.com"}) {
      StepVerifier.create(caso.comentar(ANA, "mc-gancho", malo))
        .expectErrorSatisfies(e -> {
          assertInstanceOf(TapsComentarioException.class, e);
          assertEquals(TapsComentarioException.INVALIDO, ((TapsComentarioException) e).getCode());
        }).verify();
    }
    StepVerifier.create(caso.comentar(ANA, "no-existe", "Hola")).expectError(IllegalArgumentException.class).verify();
    verify(gateway, never()).comentar(any(), any(), any(), any());
  }

  @Test
  void limitaDiezPorMinutoYCienPorDia() {
    for (int i = 0; i < 10; i++) StepVerifier.create(caso.comentar(ANA, "mc-gancho", "Hola")).expectNextCount(1).verifyComplete();
    StepVerifier.create(caso.comentar(ANA, "mc-gancho", "Hola")).expectError(TapsLimiteException.class).verify();
    verify(gateway, times(10)).comentar(any(), any(), any(), any());

    when(gateway.comentariosDesde(eq(6L), eq(AHORA.minusSeconds(86_400)))).thenReturn(Mono.just(100L));
    StepVerifier.create(caso.comentar(LUIS, "mc-gancho", "Hola")).expectError(TapsLimiteException.class).verify();
    verify(users, never()).find(6L);   // no se llama a ms-auth si el límite ya lo rechaza
  }

  @Test
  void siMsAuthFallaNoSeGuardaElComentario() {
    when(users.find(5L)).thenReturn(Mono.error(new IllegalStateException("503")));
    StepVerifier.create(caso.comentar(ANA, "mc-gancho", "Hola")).expectError(IllegalStateException.class).verify();
    verify(gateway, never()).comentar(any(), any(), any(), any());
  }

  @Test
  void paginaDeVeinteConCursor() {
    List<Comentario> filas = LongStream.rangeClosed(1, 21).map(i -> 100 - i)
      .mapToObj(id -> new Comentario(id, "mc-gancho", id == 99 ? 6L : 5L, "Ana R.", "Hola", AHORA, false)).toList();
    when(gateway.pagina("mc-gancho", 120L, 21)).thenReturn(Flux.fromIterable(filas));
    StepVerifier.create(caso.leer(LUIS, "mc-gancho", 120L)).assertNext(p -> {
      assertEquals(20, p.comentarios().size());
      assertEquals(80L, p.siguiente());
      assertTrue(p.comentarios().get(0).mio());
      assertFalse(p.comentarios().get(1).mio());
    }).verifyComplete();

    when(gateway.pagina("mc-gancho", Long.MAX_VALUE, 21)).thenReturn(Flux.fromIterable(filas.subList(0, 3)));
    StepVerifier.create(caso.leer(LUIS, "mc-gancho", null)).assertNext(p -> assertNull(p.siguiente())).verifyComplete();
    StepVerifier.create(caso.leer(LUIS, "no-existe", null)).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(caso.leer(LUIS, "mc-gancho", 0L)).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  void borraElAutorOElSuperadminNadieMas() {
    StepVerifier.create(caso.borrar(LUIS, 7)).expectError(ForbiddenException.class).verify();
    verify(gateway, never()).borrarComentario(anyLong());
    StepVerifier.create(caso.borrar(ANA, 7)).verifyComplete();
    StepVerifier.create(caso.borrar(ADMIN, 7)).verifyComplete();
    verify(gateway, times(2)).borrarComentario(7);
    StepVerifier.create(caso.borrar(ANA, 8)).expectError(TapsComentarioException.class).verify();
  }

  @Test
  void unReporteNuevoIntentaOcultarYUnoRepetidoNo() {
    StepVerifier.create(caso.reportar(ANA, 7)).verifyComplete();   // el propio no cuenta
    when(gateway.buscarComentario(10)).thenReturn(Mono.just(new Comentario(10, "mc-gancho", 5L, "Ana R.", "x", AHORA, true)));
    when(gateway.buscarComentario(11)).thenReturn(Mono.just(new Comentario(11, "u-bcdfghjkmn", 5L, "Ana R.", "x", AHORA, false)));
    StepVerifier.create(caso.reportar(LUIS, 10)).verifyComplete();   // ya oculto
    StepVerifier.create(caso.reportar(LUIS, 11)).verifyComplete();   // su Tap ya no está publicado
    verify(gateway, never()).reportar(anyLong(), any());
    when(gateway.reportar(7, 6L)).thenReturn(Mono.just(true));
    StepVerifier.create(caso.reportar(LUIS, 7)).verifyComplete();
    verify(gateway).ocultarSiReportado(7, 3);
    when(gateway.reportar(7, 6L)).thenReturn(Mono.just(false));
    StepVerifier.create(caso.reportar(LUIS, 7)).verifyComplete();
    verify(gateway, times(1)).ocultarSiReportado(anyLong(), anyInt());
    StepVerifier.create(caso.reportar(LUIS, 8)).expectError(TapsComentarioException.class).verify();
  }

  @Test
  void soloElSuperadminOcultaORestaura() {
    StepVerifier.create(caso.cambiarEstado(ANA, 7, true)).expectError(ForbiddenException.class).verify();
    StepVerifier.create(caso.cambiarEstado(ADMIN, 7, false)).verifyComplete();
    verify(gateway).cambiarOculto(7, false);
    StepVerifier.create(caso.cambiarEstado(ADMIN, 7, null)).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(caso.cambiarEstado(ADMIN, 8, true)).expectError(TapsComentarioException.class).verify();
  }
}
