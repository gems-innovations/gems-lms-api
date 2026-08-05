package com.gems.auth.application;

import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.exceptions.InvalidCredentialsException;
import com.gems.auth.application.exceptions.UserDeactivatedException;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.application.gateway.JwtGateway;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

  @Mock
  private UserGateway userGateway;

  @Mock
  private PasswordEncoderGateway passwordEncoderGateway;

  @Mock
  private JwtGateway jwtGateway;

  @InjectMocks
  private LoginUseCase loginUseCase;

  private LoginCommand validLoginCommand;
  private User activeUser;
  private User inactiveUser;

  @BeforeEach
  void setUp() {
    validLoginCommand = new LoginCommand("john.doe@example.com", "Password123!");

    LocalDateTime now = LocalDateTime.now();
    activeUser = new User(
      new UserId(1L),
      new UserName("John Doe"),
      new Email("john.doe@example.com"),
      new Password("encodedPassword123!"),
      UserRole.STUDENT,
      "inst-123",
      now,
      now,
      true
    );

    inactiveUser = new User(
      new UserId(2L),
      new UserName("Inactive User"),
      new Email("inactive@example.com"),
      new Password("encodedPassword123!"),
      UserRole.STUDENT,
      "inst-123",
      now,
      now,
      false
    );
  }

  @Test
  void shouldLoginSuccessfully() {
    // Given
    String generatedToken = "jwt.token.value";
    when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(activeUser));
    when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(true);
    when(jwtGateway.generateToken(anyLong(), anyString())).thenReturn(generatedToken);

    // When
    Mono<LoginResponse> result = loginUseCase.execute(validLoginCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.userId().equals(1L) &&
        response.name().equals("John Doe") &&
        response.email().equals("john.doe@example.com") &&
        response.role().equals("STUDENT") &&
        response.token().equals(generatedToken) &&
        response.institutionId().equals("inst-123")
      )
      .verifyComplete();

    verify(userGateway, times(1)).findByEmail(any(Email.class));
    verify(passwordEncoderGateway, times(1)).matches(anyString(), anyString());
    verify(jwtGateway, times(1)).generateToken(1L, "STUDENT");
  }

  @Test
  void shouldThrowExceptionWhenUserNotFound() {
    // Given
    when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.empty());

    // When
    Mono<LoginResponse> result = loginUseCase.execute(validLoginCommand);

    // Then
    StepVerifier.create(result)
      .expectError(UserNotFoundException.class)
      .verify();

    verify(userGateway, times(1)).findByEmail(any(Email.class));
    verify(passwordEncoderGateway, never()).matches(anyString(), anyString());
    verify(jwtGateway, never()).generateToken(anyLong(), anyString());
  }

  @Test
  void shouldThrowExceptionWhenUserIsDeactivated() {
    // Given
    when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(inactiveUser));

    // When
    Mono<LoginResponse> result = loginUseCase.execute(validLoginCommand);

    // Then
    StepVerifier.create(result)
      .expectError(UserDeactivatedException.class)
      .verify();

    verify(userGateway, times(1)).findByEmail(any(Email.class));
    verify(passwordEncoderGateway, never()).matches(anyString(), anyString());
    verify(jwtGateway, never()).generateToken(anyLong(), anyString());
  }

  @Test
  void shouldThrowExceptionWhenPasswordDoesNotMatch() {
    // Given
    when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(activeUser));
    when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(false);

    // When
    Mono<LoginResponse> result = loginUseCase.execute(validLoginCommand);

    // Then
    StepVerifier.create(result)
      .expectError(InvalidCredentialsException.class)
      .verify();

    verify(userGateway, times(1)).findByEmail(any(Email.class));
    verify(passwordEncoderGateway, times(1)).matches(anyString(), anyString());
    verify(jwtGateway, never()).generateToken(anyLong(), anyString());
  }

  @Test
  void shouldValidatePasswordCorrectly() {
    // Given
    String rawPassword = "Password123!";
    String encodedPassword = "encodedPassword123!";

    when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(activeUser));
    when(passwordEncoderGateway.matches(rawPassword, encodedPassword)).thenReturn(true);
    when(jwtGateway.generateToken(anyLong(), anyString())).thenReturn("token");

    // When
    Mono<LoginResponse> result = loginUseCase.execute(validLoginCommand);

    // Then
    StepVerifier.create(result)
      .expectNextCount(1)
      .verifyComplete();

    verify(passwordEncoderGateway, times(1)).matches(rawPassword, encodedPassword);
  }

  @Test
  void shouldGenerateTokenWithUserIdAndRole() {
    // Given
    when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(activeUser));
    when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(true);
    when(jwtGateway.generateToken(1L, "STUDENT")).thenReturn("specific.token");

    // When
    Mono<LoginResponse> result = loginUseCase.execute(validLoginCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response -> response.token().equals("specific.token"))
      .verifyComplete();

    verify(jwtGateway, times(1)).generateToken(1L, "STUDENT");
  }

  @Test
  void shouldLoginTeacherSuccessfully() {
    // Given
    User teacher = new User(
      new UserId(3L),
      new UserName("Teacher User"),
      new Email("teacher@example.com"),
      new Password("EncodedPass123!"),
      UserRole.TEACHER,
      "inst-123",
      LocalDateTime.now(),
      LocalDateTime.now(),
      true
    );
    LoginCommand teacherCommand = new LoginCommand("teacher@example.com", "Password123!");

    when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(teacher));
    when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(true);
    when(jwtGateway.generateToken(anyLong(), anyString())).thenReturn("teacher.token");

    // When
    Mono<LoginResponse> result = loginUseCase.execute(teacherCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.role().equals("TEACHER") &&
        response.userId().equals(3L) &&
        response.institutionId().equals("inst-123")
      )
      .verifyComplete();
  }

  @Test
  void shouldReturnLoginResponseWithAllFields() {
    // Given
    when(userGateway.findByEmail(any(Email.class))).thenReturn(Mono.just(activeUser));
    when(passwordEncoderGateway.matches(anyString(), anyString())).thenReturn(true);
    when(jwtGateway.generateToken(anyLong(), anyString())).thenReturn("jwt.token");

    // When
    Mono<LoginResponse> result = loginUseCase.execute(validLoginCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.userId() != null &&
        response.name() != null &&
        response.email() != null &&
        response.role() != null &&
        response.token() != null &&
        response.institutionId() != null
      )
      .verifyComplete();
  }
}