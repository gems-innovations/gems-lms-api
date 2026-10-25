package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.GoogleSignInUseCase;
import com.gems.auth.application.exceptions.GoogleSignInDisabledException;
import com.gems.auth.application.exceptions.InvalidCredentialsException;
import com.gems.auth.application.response.LoginResponse;
import com.gems.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoogleAuthControllerTest {
  private GoogleSignInUseCase useCase;
  private ReactiveAuthenticationManager sessions;
  private WebTestClient client;
  private LoginResponse response;

  @BeforeEach
  void setUp() {
    useCase = Mockito.mock(GoogleSignInUseCase.class);
    sessions = Mockito.mock(ReactiveAuthenticationManager.class);
    client = WebTestClient.bindToController(new GoogleAuthController(useCase, sessions))
      .controllerAdvice(new GlobalExceptionHandler(), new com.gems.shared.security.SecurityExceptionAdvice())
      .build()
      .mutate().responseTimeout(Duration.ofSeconds(30)).build();
    LocalDateTime now = LocalDateTime.now();
    response = new LoginResponse(1L, "Ana", "Ruiz", "ana", "ana@example.com", "STUDENT", "gems-abierto", null,
      true, now, now, "gems-token", false);
    when(useCase.isEnabled()).thenReturn(true);
  }

  private WebTestClient.ResponseSpec post(String body, String bearer) {
    WebTestClient.RequestBodySpec spec = client.post().uri("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON);
    if (bearer != null) spec.header("Authorization", "Bearer " + bearer);
    return spec.bodyValue(body).exchange();
  }

  @Test
  void signsInWithGoogleAndReturnsTheLoginResponse() {
    when(useCase.execute("tok", true, true, null)).thenReturn(Mono.just(response));

    post("{\"credential\":\"tok\",\"acceptPolicy\":true,\"tips\":true}", null)
      .expectStatus().isOk()
      .expectBody()
      .jsonPath("$.token").isEqualTo("gems-token")
      .jsonPath("$.email").isEqualTo("ana@example.com");
  }

  @Test
  void passesTheGuestBehindTheBearerToken() {
    when(sessions.authenticate(any())).thenReturn(Mono.just(new UsernamePasswordAuthenticationToken(
      new AuthenticatedUser(31L, "STUDENT", "gems-abierto"), "guest-jwt", List.of())));
    when(useCase.execute("tok", true, false, 31L)).thenReturn(Mono.just(response));

    post("{\"credential\":\"tok\",\"acceptPolicy\":true}", "guest-jwt").expectStatus().isOk();
    verify(useCase).execute("tok", true, false, 31L);
  }

  @Test
  void anInvalidBearerIsIgnored() {
    when(sessions.authenticate(any())).thenReturn(Mono.error(new BadCredentialsException("expired")));
    when(useCase.execute("tok", true, false, null)).thenReturn(Mono.just(response));

    post("{\"credential\":\"tok\",\"acceptPolicy\":true}", "stale").expectStatus().isOk();
  }

  @Test
  void answers404WhenGoogleIsNotConfigured() {
    when(useCase.isEnabled()).thenReturn(false);
    when(useCase.execute(any(), anyBoolean(), anyBoolean(), any())).thenReturn(Mono.error(new GoogleSignInDisabledException()));

    post("{\"credential\":\"tok\",\"acceptPolicy\":true}", null).expectStatus().isNotFound();
  }

  @Test
  void answers401ForAnInvalidToken() {
    when(useCase.execute(eq("bad"), anyBoolean(), anyBoolean(), any()))
      .thenReturn(Mono.error(new InvalidCredentialsException("Invalid Google credential")));

    post("{\"credential\":\"bad\",\"acceptPolicy\":true}", null).expectStatus().isUnauthorized();
  }

  @Test
  void answers400WhenThePolicyIsNotAccepted() {
    when(useCase.execute(eq("tok"), eq(false), anyBoolean(), any()))
      .thenReturn(Mono.error(new IllegalArgumentException("You must accept the data processing policy")));

    post("{\"credential\":\"tok\"}", null).expectStatus().isBadRequest();
  }
}
