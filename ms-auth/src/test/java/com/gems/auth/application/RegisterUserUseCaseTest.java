package com.gems.auth.application;

import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.exceptions.UserAlreadyExistsException;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

  @Mock
  private UserGateway userGateway;

  @Mock
  private PasswordEncoderGateway passwordEncoderGateway;

  @InjectMocks
  private RegisterUserUseCase registerUserUseCase;

  private RegisterUserCommand validCommand;
  private User savedUser;

  @BeforeEach
  void setUp() {
    validCommand = new RegisterUserCommand(
      "John Doe",
      "john.doe@example.com",
      "Password123!",
      "STUDENT"
    );

    LocalDateTime now = LocalDateTime.now();
    savedUser = new User(
      new UserId(1L),
      new UserName("John Doe"),
      new Email("john.doe@example.com"),
      new Password("encodedPassword123!"),
      UserRole.STUDENT,
      now,
      now,
      true
    );
  }

  @Test
  void shouldRegisterUserSuccessfully() {
    // Given
    when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(false));
    when(passwordEncoderGateway.encode(anyString())).thenReturn("encodedPassword123!");
    when(userGateway.save(any(User.class))).thenReturn(Mono.just(savedUser));

    // When
    Mono<UserResponse> result = registerUserUseCase.execute(validCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.userId().equals(1L) &&
        response.name().equals("John Doe") &&
        response.email().equals("john.doe@example.com") &&
        response.role().equals("STUDENT") &&
        response.active()
      )
      .verifyComplete();

    verify(userGateway, times(1)).existsByEmail(any(Email.class));
    verify(passwordEncoderGateway, times(1)).encode(anyString());
    verify(userGateway, times(1)).save(any(User.class));
  }

  @Test
  void shouldThrowExceptionWhenUserAlreadyExists() {
    // Given
    when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(true));

    // When
    Mono<UserResponse> result = registerUserUseCase.execute(validCommand);

    // Then
    StepVerifier.create(result)
      .expectError(UserAlreadyExistsException.class)
      .verify();

    verify(userGateway, times(1)).existsByEmail(any(Email.class));
    verify(passwordEncoderGateway, never()).encode(anyString());
    verify(userGateway, never()).save(any(User.class));
  }

  @Test
  void shouldRegisterUserWithTeacherRole() {
    // Given
    RegisterUserCommand teacherCommand = new RegisterUserCommand(
      "Jane Teacher",
      "jane@example.com",
      "Password123!",
      "TEACHER"
    );

    LocalDateTime now = LocalDateTime.now();
    User teacherUser = new User(
      new UserId(2L),
      new UserName("Jane Teacher"),
      new Email("jane@example.com"),
      new Password("EncodedPass123!"),
      UserRole.TEACHER,
      now,
      now,
      true
    );

    when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(false));
    when(passwordEncoderGateway.encode(anyString())).thenReturn("EncodedPass123!");
    when(userGateway.save(any(User.class))).thenReturn(Mono.just(teacherUser));

    // When
    Mono<UserResponse> result = registerUserUseCase.execute(teacherCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.role().equals("TEACHER")
      )
      .verifyComplete();
  }

  @Test
  void shouldRegisterUserWithAdminRole() {
    // Given
    RegisterUserCommand adminCommand = new RegisterUserCommand(
      "Admin User",
      "admin@example.com",
      "AdminPass123!",
      "ADMIN"
    );

    LocalDateTime now = LocalDateTime.now();
    User adminUser = new User(
      new UserId(3L),
      new UserName("Admin User"),
      new Email("admin@example.com"),
      new Password("EncodedPass123!"),
      UserRole.ADMIN,
      now,
      now,
      true
    );

    when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(false));
    when(passwordEncoderGateway.encode(anyString())).thenReturn("EncodedPass123!");
    when(userGateway.save(any(User.class))).thenReturn(Mono.just(adminUser));

    // When
    Mono<UserResponse> result = registerUserUseCase.execute(adminCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.role().equals("ADMIN")
      )
      .verifyComplete();
  }

  @Test
  void shouldEncodePasswordBeforeSaving() {
    // Given
    String rawPassword = "Password123!";
    String encodedPassword = "EncodedPass123!";

    when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(false));
    when(passwordEncoderGateway.encode(rawPassword)).thenReturn(encodedPassword);
    when(userGateway.save(any(User.class))).thenReturn(Mono.just(savedUser));

    // When
    Mono<UserResponse> result = registerUserUseCase.execute(validCommand);

    // Then
    StepVerifier.create(result)
      .expectNextCount(1)
      .verifyComplete();

    verify(passwordEncoderGateway, times(1)).encode(rawPassword);
    verify(userGateway).save(argThat(user ->
      user.getPassword().getValue().equals(encodedPassword)
    ));
  }

  @Test
  void shouldReturnUserResponseWithAllFields() {
    // Given
    when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(false));
    when(passwordEncoderGateway.encode(anyString())).thenReturn("encodedPassword123!");
    when(userGateway.save(any(User.class))).thenReturn(Mono.just(savedUser));

    // When
    Mono<UserResponse> result = registerUserUseCase.execute(validCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.userId() != null &&
        response.name() != null &&
        response.email() != null &&
        response.role() != null &&
        response.createdAt() != null &&
        response.updatedAt() != null
      )
      .verifyComplete();
  }
}