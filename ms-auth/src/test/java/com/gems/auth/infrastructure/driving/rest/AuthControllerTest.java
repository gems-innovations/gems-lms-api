package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.ChangePasswordUseCase;
import com.gems.auth.application.LoginUseCase;
import com.gems.auth.application.PasswordRecoveryUseCase;
import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.exceptions.PasswordChangeException;
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

import java.time.Duration;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

  private RegisterUserUseCase registerUserUseCase;
  private LoginUseCase loginUseCase;
  private ChangePasswordUseCase changePasswordUseCase;
  private PasswordRecoveryUseCase passwordRecoveryUseCase;
  private WebTestClient webTestClient;

  private UserResponse userResponse;
  private LoginResponse loginResponse;

  @BeforeEach
  void setUp() {
    registerUserUseCase = Mockito.mock(RegisterUserUseCase.class);
    loginUseCase = Mockito.mock(LoginUseCase.class);
    changePasswordUseCase = Mockito.mock(ChangePasswordUseCase.class);
    passwordRecoveryUseCase = Mockito.mock(PasswordRecoveryUseCase.class);

    AuthController authController = new AuthController(registerUserUseCase, loginUseCase, changePasswordUseCase,
      passwordRecoveryUseCase);
    // Reactor Netty's client is built lazily on first use; on a loaded machine (or a slow
    // /mnt/c filesystem under WSL) that cold start alone can exceed the 5s default response
    // timeout, failing whichever test happens to run first. A longer timeout keeps this test
    // class robust without masking a real hang.
    webTestClient = WebTestClient.bindToController(authController)
      .webFilter(TestSecurity.superAdmin())
      .controllerAdvice(new GlobalExceptionHandler(), new com.gems.shared.security.SecurityExceptionAdvice())
      .build()
      .mutate().responseTimeout(Duration.ofSeconds(30)).build();

    LocalDateTime now = LocalDateTime.now();
    userResponse = new UserResponse(
      1L,
      "John",
      "Doe",
      "john.doe",
      "john.doe@example.com",
      "STUDENT",
      null,
      null,
      now,
      now,
      true
    );

    loginResponse = new LoginResponse(
      1L,
      "John",
      "Doe",
      "john.doe",
      "john.doe@example.com",
      "STUDENT",
      null,
      null,
      true,
      now,
      now,
      "jwt.token.value",
      false
    );
  }

  @Test
  void shouldRegisterUserSuccessfully() {
    // Given
    RegisterUserRequest request = new RegisterUserRequest(
      "John", "Doe", "john.doe@example.com", "Password123!", "STUDENT", null
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
        Assertions.assertEquals("John", response.firstName());
        Assertions.assertEquals("Doe", response.lastName());
        Assertions.assertEquals("john.doe@example.com", response.email());
        Assertions.assertEquals("STUDENT", response.role());
        Assertions.assertTrue(response.active());
      });

    verify(registerUserUseCase, times(1)).execute(any(RegisterUserCommand.class));
  }

  @Test
  void shouldRegisterUserWithInstructorRole() {
    // Given
    RegisterUserRequest instructorRequest = new RegisterUserRequest(
      "Jane", "Instructor", "jane@example.com", "Password123!", "INSTRUCTOR", null
    );
    UserResponse instructorResponse = new UserResponse(
      2L, "Jane", "Instructor", "jane.instructor", "jane@example.com", "INSTRUCTOR", null, null,
      LocalDateTime.now(), LocalDateTime.now(), true
    );
    when(registerUserUseCase.execute(any(RegisterUserCommand.class))).thenReturn(Mono.just(instructorResponse));

    // When & Then
    webTestClient
      .post()
      .uri("/api/v1/auth/register")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(instructorRequest)
      .exchange()
      .expectStatus().isCreated()
      .expectBody(UserResponse.class)
      .value(response -> {
        Assertions.assertEquals("INSTRUCTOR", response.role());
        Assertions.assertEquals("Jane", response.firstName());
      });

    verify(registerUserUseCase, times(1)).execute(any(RegisterUserCommand.class));
  }

  @Test
  void shouldRegisterUserWithAdminRole() {
    // Given
    RegisterUserRequest adminRequest = new RegisterUserRequest(
      "Admin", "User", "admin@example.com", "AdminPass123!", "ADMIN", null
    );
    UserResponse adminResponse = new UserResponse(
      3L, "Admin", "User", "admin.user", "admin@example.com", "ADMIN", null, null,
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
        Assertions.assertEquals("Admin", response.firstName());
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
      "John", "Doe", "john.doe@example.com", "Password123!", "STUDENT", null
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
      "John", "Doe", "john.doe@example.com", "Password123!", "STUDENT", null
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
        Assertions.assertNotNull(response.firstName());
        Assertions.assertNotNull(response.lastName());
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
        Assertions.assertNotNull(response.firstName());
        Assertions.assertNotNull(response.lastName());
        Assertions.assertNotNull(response.email());
        Assertions.assertNotNull(response.role());
        Assertions.assertNotNull(response.token());
      });
  }

  @Test
  void changePasswordUsesTheCallersId() {
    when(changePasswordUseCase.execute(9999L, "Old123!a", "New123!a")).thenReturn(Mono.empty());

    webTestClient.post().uri("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"currentPassword\":\"Old123!a\",\"newPassword\":\"New123!a\"}")
      .exchange().expectStatus().isNoContent();
  }

  @Test
  void wrongCurrentPasswordIsABadRequestNotAnAuthFailure() {
    when(changePasswordUseCase.execute(anyLong(), any(), any())).thenReturn(Mono.error(
      new PasswordChangeException(PasswordChangeException.WRONG_CURRENT_PASSWORD, "wrong")));

    webTestClient.post().uri("/api/v1/auth/change-password").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"currentPassword\":\"x\",\"newPassword\":\"New123!a\"}")
      .exchange().expectStatus().isBadRequest()
      .expectBody().jsonPath("$.code").isEqualTo("WRONG_CURRENT_PASSWORD");
  }

  @Test
  void forgotPasswordAlwaysAnswersAccepted() {
    when(passwordRecoveryUseCase.requestReset("nobody@example.com")).thenReturn(Mono.empty());

    webTestClient.post().uri("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"email\":\"nobody@example.com\"}")
      .exchange().expectStatus().isAccepted();
  }

  @Test
  void resetPasswordRejectsInvalidTokens() {
    when(passwordRecoveryUseCase.reset("bad", "New123!a")).thenReturn(Mono.error(
      new PasswordChangeException(PasswordChangeException.INVALID_RESET_TOKEN, "invalid")));

    webTestClient.post().uri("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"token\":\"bad\",\"newPassword\":\"New123!a\"}")
      .exchange().expectStatus().isBadRequest()
      .expectBody().jsonPath("$.code").isEqualTo("INVALID_RESET_TOKEN");
  }
}
