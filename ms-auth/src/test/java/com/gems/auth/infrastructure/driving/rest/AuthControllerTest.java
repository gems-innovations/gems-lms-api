package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.LoginUseCase;
import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.infrastructure.driving.rest.request.LoginRequest;
import com.gems.auth.infrastructure.driving.rest.request.RegisterUserRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

  private RegisterUserUseCase registerUserUseCase;
  private LoginUseCase loginUseCase;
  private WebTestClient webTestClient;

  private UserResponse userResponse;
  private LoginResponse loginResponse;

  @BeforeEach
  void setUp() {
    registerUserUseCase = Mockito.mock(RegisterUserUseCase.class);
    loginUseCase = Mockito.mock(LoginUseCase.class);

    AuthController authController = new AuthController(registerUserUseCase, loginUseCase);
    webTestClient = WebTestClient.bindToController(authController).build();

    LocalDateTime now = LocalDateTime.now();
    userResponse = new UserResponse(
      1L,
      "John Doe",
      "john.doe@example.com",
      "STUDENT",
      now,
      now,
      true
    );

    loginResponse = new LoginResponse(
      1L,
      "John Doe",
      "john.doe@example.com",
      "STUDENT",
      "jwt.token.value"
    );
  }

  @Test
  void shouldRegisterUserSuccessfully() {
    // Given
    RegisterUserRequest request = new RegisterUserRequest(
      "John Doe",
      "john.doe@example.com",
      "Password123!",
      "STUDENT"
    );
    when(registerUserUseCase.execute(any(RegisterUserCommand.class))).thenReturn(Mono.just(userResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/auth/register")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isCreated()
      .expectBody(UserResponse.class)
      .value(response -> {
        Assertions.assertEquals(1L, response.userId());
        Assertions.assertEquals("John Doe", response.name());
        Assertions.assertEquals("john.doe@example.com", response.email());
        Assertions.assertEquals("STUDENT", response.role());
        Assertions.assertTrue(response.active());
      });

    verify(registerUserUseCase, times(1)).execute(any(RegisterUserCommand.class));
  }

  @Test
  void shouldRegisterUserWithTeacherRole() {
    // Given
    RegisterUserRequest teacherRequest = new RegisterUserRequest(
      "Jane Teacher",
      "jane@example.com",
      "Password123!",
      "TEACHER"
    );
    UserResponse teacherResponse = new UserResponse(
      2L, "Jane Teacher", "jane@example.com", "TEACHER",
      LocalDateTime.now(), LocalDateTime.now(), true
    );
    when(registerUserUseCase.execute(any(RegisterUserCommand.class))).thenReturn(Mono.just(teacherResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/auth/register")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(teacherRequest)
      .exchange()
      .expectStatus().isCreated()
      .expectBody(UserResponse.class)
      .value(response -> {
        Assertions.assertEquals("TEACHER", response.role());
        Assertions.assertEquals("Jane Teacher", response.name());
      });

    verify(registerUserUseCase, times(1)).execute(any(RegisterUserCommand.class));
  }

  @Test
  void shouldRegisterUserWithAdminRole() {
    // Given
    RegisterUserRequest adminRequest = new RegisterUserRequest(
      "Admin User",
      "admin@example.com",
      "AdminPass123!",
      "ADMIN"
    );
    UserResponse adminResponse = new UserResponse(
      3L, "Admin User", "admin@example.com", "ADMIN",
      LocalDateTime.now(), LocalDateTime.now(), true
    );
    when(registerUserUseCase.execute(any(RegisterUserCommand.class))).thenReturn(Mono.just(adminResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/auth/register")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(adminRequest)
      .exchange()
      .expectStatus().isCreated()
      .expectBody(UserResponse.class)
      .value(response -> {
        Assertions.assertEquals("ADMIN", response.role());
        Assertions.assertEquals("Admin User", response.name());
      });

    verify(registerUserUseCase, times(1)).execute(any(RegisterUserCommand.class));
  }

  @Test
  void shouldLoginSuccessfully() {
    // Given
    LoginRequest request = new LoginRequest("john.doe@example.com", "Password123!");
    when(loginUseCase.execute(any(LoginCommand.class))).thenReturn(Mono.just(loginResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk()
      .expectBody(LoginResponse.class)
      .value(response -> {
        Assertions.assertEquals(1L, response.userId());
        Assertions.assertEquals("john.doe@example.com", response.email());
        Assertions.assertEquals("jwt.token.value", response.token());
        Assertions.assertEquals("STUDENT", response.role());
      });

    verify(loginUseCase, times(1)).execute(any(LoginCommand.class));
  }

  @Test
  void shouldReturnTokenOnSuccessfulLogin() {
    // Given
    LoginRequest request = new LoginRequest("john.doe@example.com", "Password123!");
    when(loginUseCase.execute(any(LoginCommand.class))).thenReturn(Mono.just(loginResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk()
      .expectBody(LoginResponse.class)
      .value(response -> {
        Assertions.assertNotNull(response.token());
        Assertions.assertFalse(response.token().isEmpty());
      });
  }

  @Test
  void shouldReturnOkStatusOnLogin() {
    // Given
    LoginRequest request = new LoginRequest("john.doe@example.com", "Password123!");
    when(loginUseCase.execute(any(LoginCommand.class))).thenReturn(Mono.just(loginResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk();

    verify(loginUseCase, times(1)).execute(any(LoginCommand.class));
  }

  @Test
  void shouldReturnCreatedStatusOnRegister() {
    // Given
    RegisterUserRequest request = new RegisterUserRequest(
      "John Doe",
      "john.doe@example.com",
      "Password123!",
      "STUDENT"
    );
    when(registerUserUseCase.execute(any(RegisterUserCommand.class))).thenReturn(Mono.just(userResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/auth/register")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isCreated();

    verify(registerUserUseCase, times(1)).execute(any(RegisterUserCommand.class));
  }

  @Test
  void shouldReturnUserResponseBodyOnRegister() {
    // Given
    RegisterUserRequest request = new RegisterUserRequest(
      "John Doe",
      "john.doe@example.com",
      "Password123!",
      "STUDENT"
    );
    when(registerUserUseCase.execute(any(RegisterUserCommand.class))).thenReturn(Mono.just(userResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/auth/register")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isCreated()
      .expectBody(UserResponse.class)
      .value(response -> {
        Assertions.assertNotNull(response.userId());
        Assertions.assertNotNull(response.name());
        Assertions.assertNotNull(response.email());
        Assertions.assertNotNull(response.role());
      });
  }

  @Test
  void shouldReturnLoginResponseBodyOnLogin() {
    // Given
    LoginRequest request = new LoginRequest("john.doe@example.com", "Password123!");
    when(loginUseCase.execute(any(LoginCommand.class))).thenReturn(Mono.just(loginResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/auth/login")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk()
      .expectBody(LoginResponse.class)
      .value(response -> {
        Assertions.assertNotNull(response.userId());
        Assertions.assertNotNull(response.name());
        Assertions.assertNotNull(response.email());
        Assertions.assertNotNull(response.role());
        Assertions.assertNotNull(response.token());
      });
  }
}

