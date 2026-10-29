package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.TapsComentariosUseCase;
import com.gems.education.application.TapsSocialesUseCase;
import com.gems.education.application.exceptions.TapsComentarioException;
import com.gems.education.application.exceptions.TapsLimiteException;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.ForbiddenException;
import com.gems.shared.security.SecurityExceptionAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TapsSocialesControllerTest {
  private static final AuthenticatedUser ANA = new AuthenticatedUser(5L, "STUDENT", "inst-1");
  private final TapsSocialesUseCase sociales = mock(TapsSocialesUseCase.class);
  private final TapsComentariosUseCase comentarios = mock(TapsComentariosUseCase.class);
  private final TapsSocialesController controller = new TapsSocialesController(sociales, comentarios);

  private WebTestClient anonimo() {
    return WebTestClient.bindToController(controller)
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice()).build();
  }

  private WebTestClient como(AuthenticatedUser u) {
    return WebTestClient.bindToController(controller).webFilter(TestSecurity.authenticatedAs(u))
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice()).build();
  }

  @Test
  void lasCifrasSonPublicasYSeCachean60s() {
    when(sociales.leer("mc-gancho,fleming")).thenReturn(Mono.just(new TapsSocialesUseCase.Sociales(
      List.of(new TapsSocialesUseCase.Social("mc-gancho", 12, 3, 2)))));
    anonimo().get().uri("/api/v1/public/taps/sociales?ids=mc-gancho,fleming").exchange().expectStatus().isOk()
      .expectHeader().cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
      .expectBody().json("{\"taps\":[{\"tapId\":\"mc-gancho\",\"vistas\":12,\"meGusta\":3,\"comentarios\":2}]}");
  }

  @Test
  void sinSesionLoPersonalResponde403() {
    anonimo().get().uri("/api/v1/taps/sociales/yo?ids=mc-gancho").exchange().expectStatus().isForbidden();
    anonimo().put().uri("/api/v1/taps/mc-gancho/me-gusta").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"meGusta\":true}").exchange().expectStatus().isForbidden();
    anonimo().get().uri("/api/v1/taps/mc-gancho/comentarios").exchange().expectStatus().isForbidden();
    verifyNoInteractions(sociales, comentarios);
  }

  @Test
  void elMeGustaUsaElUsuarioDelTokenYNoSeCachea() {
    when(sociales.cambiar(5L, "mc-gancho", true)).thenReturn(Mono.just(new TapsSocialesUseCase.Estado("mc-gancho", true, 4)));
    como(ANA).put().uri("/api/v1/taps/mc-gancho/me-gusta").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"meGusta\":true,\"userId\":99}").exchange().expectStatus().isOk()
      .expectHeader().cacheControl(CacheControl.noStore().cachePrivate())
      .expectBody().json("{\"tapId\":\"mc-gancho\",\"meGusta\":true,\"total\":4}");
    when(sociales.mios(5L, "mc-gancho")).thenReturn(Mono.just(new TapsSocialesUseCase.Mios(
      List.of(new TapsSocialesUseCase.MeGustaYo("mc-gancho", true)))));
    como(ANA).get().uri("/api/v1/taps/sociales/yo?ids=mc-gancho").exchange().expectStatus().isOk()
      .expectHeader().cacheControl(CacheControl.noStore().cachePrivate())
      .expectBody().jsonPath("$.taps[0].meGustaYo").isEqualTo(true);
  }

  @Test
  void erroresDelMeGusta() {
    when(sociales.cambiar(any(), any(), any())).thenReturn(Mono.error(new IllegalArgumentException("Tap desconocido")));
    como(ANA).put().uri("/api/v1/taps/no-existe/me-gusta").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"meGusta\":true}").exchange().expectStatus().isBadRequest();
    when(sociales.cambiar(any(), any(), any())).thenReturn(Mono.error(new TapsLimiteException("espera")));
    como(ANA).put().uri("/api/v1/taps/mc-gancho/me-gusta").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"meGusta\":true}").exchange().expectStatus().isEqualTo(429);
    como(ANA).put().uri("/api/v1/taps/mc-gancho/me-gusta").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"meGusta\":\"quizas\"}").exchange().expectStatus().isBadRequest();
  }

  @Test
  void comentarYLosErrores() {
    when(comentarios.comentar(ANA, "mc-gancho", "Hola"))
      .thenReturn(Mono.just(new TapsComentariosUseCase.Vista(7, "Ana R.", "Hola", Instant.parse("2026-10-29T15:00:00Z"), true)));
    como(ANA).post().uri("/api/v1/taps/mc-gancho/comentarios").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"texto\":\"Hola\"}").exchange().expectStatus().isCreated()
      .expectHeader().cacheControl(CacheControl.noStore())
      .expectBody().json("{\"id\":7,\"autor\":\"Ana R.\",\"texto\":\"Hola\",\"mio\":true}")
      .jsonPath("$.userId").doesNotExist();

    when(comentarios.comentar(ANA, "mc-gancho", "www.x.com")).thenReturn(Mono.error(TapsComentarioException.invalido("No se admiten enlaces en el comentario")));
    como(ANA).post().uri("/api/v1/taps/mc-gancho/comentarios").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"texto\":\"www.x.com\"}").exchange().expectStatus().isEqualTo(422)
      .expectBody().jsonPath("$.message").isEqualTo("No se admiten enlaces en el comentario");
  }

  @Test
  void leerComentariosNoSeCachea() {
    when(comentarios.leer(ANA, "mc-gancho", 50L)).thenReturn(Mono.just(new TapsComentariosUseCase.Pagina(List.of(), null)));
    como(ANA).get().uri("/api/v1/taps/mc-gancho/comentarios?antes=50").exchange().expectStatus().isOk()
      .expectHeader().cacheControl(CacheControl.noStore().cachePrivate())
      .expectBody().json("{\"comentarios\":[],\"siguiente\":null}");
  }

  @Test
  void borrarReportarYModerar() {
    when(comentarios.borrar(ANA, 7)).thenReturn(Mono.empty());
    como(ANA).delete().uri("/api/v1/taps/comentarios/7").exchange().expectStatus().isNoContent();
    when(comentarios.borrar(ANA, 8)).thenReturn(Mono.error(new ForbiddenException("no")));
    como(ANA).delete().uri("/api/v1/taps/comentarios/8").exchange().expectStatus().isForbidden();
    when(comentarios.reportar(ANA, 9)).thenReturn(Mono.error(TapsComentarioException.noExiste()));
    como(ANA).post().uri("/api/v1/taps/comentarios/9/reportar").exchange().expectStatus().isNotFound();
    when(comentarios.cambiarEstado(ANA, 7, true)).thenReturn(Mono.error(new ForbiddenException("no")));
    como(ANA).put().uri("/api/v1/taps/comentarios/7/estado").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"oculto\":true}").exchange().expectStatus().isForbidden();
  }
}
