package com.gems.auth.infrastructure.driven.postgresql;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

  @Mock
  private IUserRepository userRepository;

  @InjectMocks
  private UserRepositoryAdapter userRepositoryAdapter;

  private User testUser;
  private UserEntity testUserEntity;

  @BeforeEach
  void setUp() {
    LocalDateTime now = LocalDateTime.now();

    testUser = new User(
      new UserId(1L),
      new UserName("Test User"),
      new Email("test@example.com"),
      new Password("Password123!"),
      UserRole.STUDENT,
      now,
      now,
      true
    );

    testUserEntity = new UserEntity(
      1L,
      "Test User",
      "test@example.com",
      "Password123!",
      "STUDENT",
      true,
      now,
      now
    );
  }

  @Test
  void shouldSaveUser() {
    // Given
    when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(testUserEntity));

    // When
    Mono<User> result = userRepositoryAdapter.save(testUser);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(user ->
        user.getId().getValue().equals(1L) &&
        user.getName().getValue().equals("Test User") &&
        user.getEmail().getValue().equals("test@example.com")
      )
      .verifyComplete();

    verify(userRepository, times(1)).save(any(UserEntity.class));
  }

  @Test
  void shouldSaveUserWithoutId() {
    // Given
    User userWithoutId = new User("New User", "new@example.com", "Password123!", UserRole.TEACHER);
    UserEntity savedEntity = new UserEntity(
      2L, "New User", "new@example.com", "Password123!", "TEACHER",
      true, LocalDateTime.now(), LocalDateTime.now()
    );
    when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(savedEntity));

    // When
    Mono<User> result = userRepositoryAdapter.save(userWithoutId);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(user ->
        user.getId().getValue().equals(2L) &&
        user.getName().getValue().equals("New User")
      )
      .verifyComplete();
  }

  @Test
  void shouldFindUserById() {
    // Given
    UserId userId = new UserId(1L);
    when(userRepository.findById(1L)).thenReturn(Mono.just(testUserEntity));

    // When
    Mono<User> result = userRepositoryAdapter.findById(userId);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(user ->
        user.getId().getValue().equals(1L) &&
        user.getEmail().getValue().equals("test@example.com")
      )
      .verifyComplete();

    verify(userRepository, times(1)).findById(1L);
  }

  @Test
  void shouldFindUserByEmail() {
    // Given
    Email email = new Email("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Mono.just(testUserEntity));

    // When
    Mono<User> result = userRepositoryAdapter.findByEmail(email);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(user ->
        user.getEmail().getValue().equals("test@example.com")
      )
      .verifyComplete();

    verify(userRepository, times(1)).findByEmail("test@example.com");
  }

  @Test
  void shouldCheckIfEmailExists() {
    // Given
    Email email = new Email("test@example.com");
    when(userRepository.existsByEmail("test@example.com")).thenReturn(Mono.just(true));

    // When
    Mono<Boolean> result = userRepositoryAdapter.existsByEmail(email);

    // Then
    StepVerifier.create(result)
      .expectNext(true)
      .verifyComplete();

    verify(userRepository, times(1)).existsByEmail("test@example.com");
  }

  @Test
  void shouldCheckIfEmailDoesNotExist() {
    // Given
    Email email = new Email("nonexistent@example.com");
    when(userRepository.existsByEmail("nonexistent@example.com")).thenReturn(Mono.just(false));

    // When
    Mono<Boolean> result = userRepositoryAdapter.existsByEmail(email);

    // Then
    StepVerifier.create(result)
      .expectNext(false)
      .verifyComplete();
  }

  @Test
  void shouldDeleteUserById() {
    // Given
    UserId userId = new UserId(1L);
    when(userRepository.deleteById(1L)).thenReturn(Mono.empty());

    // When
    Mono<Void> result = userRepositoryAdapter.deleteById(userId);

    // Then
    StepVerifier.create(result)
      .verifyComplete();

    verify(userRepository, times(1)).deleteById(1L);
  }

  @Test
  void shouldMapToDomainCorrectly() {
    // Given
    when(userRepository.findById(anyLong())).thenReturn(Mono.just(testUserEntity));

    // When
    Mono<User> result = userRepositoryAdapter.findById(new UserId(1L));

    // Then
    StepVerifier.create(result)
      .expectNextMatches(user ->
        user.getId().getValue().equals(testUserEntity.getUserId()) &&
        user.getName().getValue().equals(testUserEntity.getName()) &&
        user.getEmail().getValue().equals(testUserEntity.getEmail()) &&
        user.getPassword().getValue().equals(testUserEntity.getPassword()) &&
        user.getRole().name().equals(testUserEntity.getRole()) &&
        user.isActive().equals(testUserEntity.isActive())
      )
      .verifyComplete();
  }

  @Test
  void shouldMapToEntityCorrectly() {
    // Given
    when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
      UserEntity entity = invocation.getArgument(0);
      return Mono.just(entity);
    });

    // When
    Mono<User> result = userRepositoryAdapter.save(testUser);

    // Then
    StepVerifier.create(result)
      .expectNextCount(1)
      .verifyComplete();

    verify(userRepository).save(argThat(entity ->
      entity.getUserId().equals(testUser.getId().getValue()) &&
      entity.getName().equals(testUser.getName().getValue()) &&
      entity.getEmail().equals(testUser.getEmail().getValue()) &&
      entity.getPassword().equals(testUser.getPassword().getValue()) &&
      entity.getRole().equals(testUser.getRole().name()) &&
      entity.isActive() == testUser.isActive()
    ));
  }
}