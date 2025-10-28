package com.gems.auth.infrastructure.driven.postgresql;

import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepositoryAdapter Tests")
class UserRepositoryAdapterTest {

    @Mock
    private IUserRepository userRepository;

    private UserRepositoryAdapter userRepositoryAdapter;

    @BeforeEach
    void setUp() {
        userRepositoryAdapter = new UserRepositoryAdapter(userRepository);
    }

    @Nested
    @DisplayName("Save Tests")
    class SaveTests {

        @Test
        @DisplayName("Should save user successfully")
        void shouldSaveUserSuccessfully() {
            final User user = createTestUser();
            UserEntity userEntity = createTestUserEntity();

            when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(userEntity));

            StepVerifier.create(userRepositoryAdapter.save(user))
                .expectNextCount(1)
                .verifyComplete();

            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should save user with null ID")
        void shouldSaveUserWithNullId() {
            User user = createTestUser();
            user = new User(
                null,
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.isActive()
            );
            UserEntity userEntity = createTestUserEntity();

            when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(userEntity));

            StepVerifier.create(userRepositoryAdapter.save(user))
                .expectNextCount(1)
                .verifyComplete();

            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should save teacher user")
        void shouldSaveTeacherUser() {
            User teacher = new User(
                new UserId(2L),
                new UserName("Jane Teacher"),
                new Email("teacher@example.com"),
                new Password("EncodedPass123!"),
                UserRole.TEACHER,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
            );

            UserEntity teacherEntity = new UserEntity(
                2L,
                "Jane Teacher",
                "teacher@example.com",
                "EncodedPass123!",
                "TEACHER",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
            );

            when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(teacherEntity));

            StepVerifier.create(userRepositoryAdapter.save(teacher))
                .expectNextCount(1)
                .verifyComplete();

            verify(userRepository).save(any(UserEntity.class));
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find user by ID successfully")
        void shouldFindUserByIdSuccessfully() {
            UserId userId = new UserId(1L);
            UserEntity userEntity = createTestUserEntity();

            when(userRepository.findById(userId.getValue())).thenReturn(Mono.just(userEntity));

            StepVerifier.create(userRepositoryAdapter.findById(userId))
                .expectNextCount(1)
                .verifyComplete();

            verify(userRepository).findById(userId.getValue());
        }

        @Test
        @DisplayName("Should return empty when user not found by ID")
        void shouldReturnEmptyWhenUserNotFoundById() {
            UserId userId = new UserId(999L);

            when(userRepository.findById(userId.getValue())).thenReturn(Mono.empty());

            StepVerifier.create(userRepositoryAdapter.findById(userId))
                .verifyComplete();

            verify(userRepository).findById(userId.getValue());
        }
    }

    @Nested
    @DisplayName("Find By Email Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should find user by email successfully")
        void shouldFindUserByEmailSuccessfully() {
            Email email = new Email("john.doe@example.com");
            UserEntity userEntity = createTestUserEntity();

            when(userRepository.findByEmail(email.getValue())).thenReturn(Mono.just(userEntity));

            StepVerifier.create(userRepositoryAdapter.findByEmail(email))
                .expectNextCount(1)
                .verifyComplete();

            verify(userRepository).findByEmail(email.getValue());
        }

        @Test
        @DisplayName("Should return empty when user not found by email")
        void shouldReturnEmptyWhenUserNotFoundByEmail() {
            Email email = new Email("nonexistent@example.com");

            when(userRepository.findByEmail(email.getValue())).thenReturn(Mono.empty());

            StepVerifier.create(userRepositoryAdapter.findByEmail(email))
                .verifyComplete();

            verify(userRepository).findByEmail(email.getValue());
        }

        @Test
        @DisplayName("Should find user with special characters in email")
        void shouldFindUserWithSpecialCharactersInEmail() {
            Email email = new Email("user+tag@example-domain.com");
            UserEntity userEntity = new UserEntity(
                1L,
                "John Doe",
                "user+tag@example-domain.com",
                "EncodedPass123!",
                "STUDENT",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
            );

            when(userRepository.findByEmail(email.getValue())).thenReturn(Mono.just(userEntity));

            StepVerifier.create(userRepositoryAdapter.findByEmail(email))
                .expectNextMatches(user -> 
                    user.getEmail().getValue().equals(email.getValue())
                )
                .verifyComplete();

            verify(userRepository).findByEmail(email.getValue());
        }
    }

    @Nested
    @DisplayName("Exists By Email Tests")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Should return true when user exists by email")
        void shouldReturnTrueWhenUserExistsByEmail() {
            Email email = new Email("john.doe@example.com");

            when(userRepository.existsByEmail(email.getValue())).thenReturn(Mono.just(true));

            StepVerifier.create(userRepositoryAdapter.existsByEmail(email))
                .expectNext(true)
                .verifyComplete();

            verify(userRepository).existsByEmail(email.getValue());
        }

        @Test
        @DisplayName("Should return false when user does not exist by email")
        void shouldReturnFalseWhenUserDoesNotExistByEmail() {
            Email email = new Email("nonexistent@example.com");

            when(userRepository.existsByEmail(email.getValue())).thenReturn(Mono.just(false));

            StepVerifier.create(userRepositoryAdapter.existsByEmail(email))
                .expectNext(false)
                .verifyComplete();

            verify(userRepository).existsByEmail(email.getValue());
        }
    }

    @Nested
    @DisplayName("Delete By ID Tests")
    class DeleteByIdTests {

        @Test
        @DisplayName("Should delete user by ID successfully")
        void shouldDeleteUserByIdSuccessfully() {
            UserId userId = new UserId(1L);

            when(userRepository.deleteById(userId.getValue())).thenReturn(Mono.empty());

            StepVerifier.create(userRepositoryAdapter.deleteById(userId))
                .verifyComplete();

            verify(userRepository).deleteById(userId.getValue());
        }

        @Test
        @DisplayName("Should handle deletion of non-existent user")
        void shouldHandleDeletionOfNonExistentUser() {
            UserId userId = new UserId(999L);

            when(userRepository.deleteById(userId.getValue())).thenReturn(Mono.empty());

            StepVerifier.create(userRepositoryAdapter.deleteById(userId))
                .verifyComplete();

            verify(userRepository).deleteById(userId.getValue());
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should save and retrieve user correctly")
        void shouldSaveAndRetrieveUserCorrectly() {
            final User user = createTestUser();
            UserEntity userEntity = createTestUserEntity();

            when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(userEntity));
            when(userRepository.findById(user.getId().getValue())).thenReturn(Mono.just(userEntity));

            StepVerifier.create(userRepositoryAdapter.save(user))
                .expectNextCount(1)
                .verifyComplete();

            StepVerifier.create(userRepositoryAdapter.findById(user.getId()))
                .expectNextCount(1)
                .verifyComplete();

            verify(userRepository).save(any(UserEntity.class));
            verify(userRepository).findById(user.getId().getValue());
        }

        @Test
        @DisplayName("Should find user by email after saving")
        void shouldFindUserByEmailAfterSaving() {
            final User user = createTestUser();
            UserEntity userEntity = createTestUserEntity();

            when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(userEntity));
            when(userRepository.findByEmail(user.getEmail().getValue())).thenReturn(Mono.just(userEntity));

            StepVerifier.create(userRepositoryAdapter.save(user))
                .expectNextCount(1)
                .verifyComplete();

            StepVerifier.create(userRepositoryAdapter.findByEmail(user.getEmail()))
                .expectNextCount(1)
                .verifyComplete();

            verify(userRepository).save(any(UserEntity.class));
            verify(userRepository).findByEmail(user.getEmail().getValue());
        }

        @Test
        @DisplayName("Should check existence by email correctly")
        void shouldCheckExistenceByEmailCorrectly() {
            Email email = new Email("john.doe@example.com");

            when(userRepository.existsByEmail(email.getValue())).thenReturn(Mono.just(true));

            StepVerifier.create(userRepositoryAdapter.existsByEmail(email))
                .expectNext(true)
                .verifyComplete();

            verify(userRepository).existsByEmail(email.getValue());
        }

        @Test
        @DisplayName("Should delete user by ID correctly")
        void shouldDeleteUserByIdCorrectly() {
            UserId userId = new UserId(1L);

            when(userRepository.deleteById(userId.getValue())).thenReturn(Mono.empty());

            StepVerifier.create(userRepositoryAdapter.deleteById(userId))
                .verifyComplete();

            verify(userRepository).deleteById(userId.getValue());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle user with special characters in name")
        void shouldHandleUserWithSpecialCharactersInName() {
            final User user = new User(
                new UserId(1L),
                new UserName("José María O'Connor-Smith"),
                new Email("jose.maria@example.com"),
                new Password("EncodedPass123!"),
                UserRole.STUDENT,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
            );

            UserEntity userEntity = createTestUserEntity();
            userEntity.setName("José María O'Connor-Smith");

            when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(userEntity));

            StepVerifier.create(userRepositoryAdapter.save(user))
                .expectNextCount(1)
                .verifyComplete();

            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should handle user with special characters in email")
        void shouldHandleUserWithSpecialCharactersInEmail() {
            final User user = new User(
                new UserId(1L),
                new UserName("John Doe"),
                new Email("user+tag@example-domain.com"),
                new Password("EncodedPass123!"),
                UserRole.STUDENT,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
            );

            UserEntity userEntity = createTestUserEntity();
            userEntity.setEmail("user+tag@example-domain.com");

            when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(userEntity));

            StepVerifier.create(userRepositoryAdapter.save(user))
                .expectNextCount(1)
                .verifyComplete();

            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("Should handle user with special characters in password")
        void shouldHandleUserWithSpecialCharactersInPassword() {
            final User user = new User(
                new UserId(1L),
                new UserName("John Doe"),
                new Email("john.doe@example.com"),
                new Password("EncodedPass123!@#$%^&*()"),
                UserRole.STUDENT,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
            );

            UserEntity userEntity = createTestUserEntity();
            userEntity.setPassword("EncodedPass123!@#$%^&*()");

            when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(userEntity));

            StepVerifier.create(userRepositoryAdapter.save(user))
                .expectNextCount(1)
                .verifyComplete();

            verify(userRepository).save(any(UserEntity.class));
        }
    }

    private User createTestUser() {
        return new User(
            new UserId(1L),
            new UserName("John Doe"),
            new Email("john.doe@example.com"),
            new Password("EncodedPass123!"),
            UserRole.STUDENT,
            LocalDateTime.now(),
            LocalDateTime.now(),
            true
        );
    }

    private UserEntity createTestUserEntity() {
        return new UserEntity(
            1L,
            "John Doe",
            "john.doe@example.com",
            "EncodedPass123!",
            "STUDENT",
            true,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
