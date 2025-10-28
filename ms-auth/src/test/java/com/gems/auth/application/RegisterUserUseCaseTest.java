package com.gems.auth.application;

import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.exceptions.UserAlreadyExistsException;
import com.gems.auth.domain.values.*;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserUseCase Tests")
class RegisterUserUseCaseTest {

    @Mock
    private UserGateway userGateway;

    @Mock
    private PasswordEncoderGateway passwordEncoderGateway;

    private RegisterUserUseCase registerUserUseCase;

    @BeforeEach
    void setUp() {
        registerUserUseCase = new RegisterUserUseCase(userGateway, passwordEncoderGateway);
    }

    @Nested
    @DisplayName("Successful Registration Tests")
    class SuccessfulRegistrationTests {

        @Test
        @DisplayName("Should return user response when registration is successful")
        void shouldReturnUserResponseWhenRegistrationIsSuccessful() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String encodedPassword = "EncodedPass123!";
            Long userId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime updatedAt = LocalDateTime.now();

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");
            User savedUser = new User(
                new UserId(userId),
                new UserName(name),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            );

            UserResponse expectedResponse = new UserResponse(
                userId,
                new UserName(name),
                new Email(email),
                UserRole.STUDENT,
                createdAt,
                updatedAt,
                true
            );

            when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(false));
            when(passwordEncoderGateway.encode(anyString())).thenReturn(encodedPassword);
            when(userGateway.save(any(User.class))).thenReturn(Mono.just(savedUser));

            StepVerifier.create(registerUserUseCase.execute(command))
                .expectNextMatches(response -> 
                    response.getId().equals(userId) &&
                    response.getName().equals(name) &&
                    response.getEmail().equals(email) &&
                    response.isActive()
                )
                .verifyComplete();

            verify(userGateway).existsByEmail(any(Email.class));
            verify(passwordEncoderGateway).encode(password);
            verify(userGateway).save(any(User.class));
        }

        @Test
        @DisplayName("Should create user with STUDENT role by default")
        void shouldCreateUserWithStudentRoleByDefault() {
            String name = "Jane Doe";
            String email = "jane.doe@example.com";
            String password = "SecurePass123!";
            String encodedPassword = "EncodedPass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(false));
            when(passwordEncoderGateway.encode(anyString())).thenReturn(encodedPassword);
            when(userGateway.save(any(User.class))).thenReturn(Mono.just(new User(
                new UserId(1L),
                new UserName(name),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            )));

            StepVerifier.create(registerUserUseCase.execute(command))
                .expectNextCount(1)
                .verifyComplete();

            verify(userGateway).save(argThat(user -> 
                user.getRole() == UserRole.STUDENT
            ));
        }
    }

    @Nested
    @DisplayName("User Already Exists Tests")
    class UserAlreadyExistsTests {

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when user exists")
        void shouldThrowUserAlreadyExistsExceptionWhenUserExists() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(true));

            StepVerifier.create(registerUserUseCase.execute(command))
                .expectError(UserAlreadyExistsException.class)
                .verify();

            verify(userGateway).existsByEmail(any(Email.class));
            verifyNoMoreInteractions(userGateway);
            verifyNoInteractions(passwordEncoderGateway);
        }

        @Test
        @DisplayName("Should include email in exception message")
        void shouldIncludeEmailInExceptionMessage() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(true));

            StepVerifier.create(registerUserUseCase.execute(command))
                .expectErrorMatches(throwable -> 
                    throwable instanceof UserAlreadyExistsException &&
                    throwable.getMessage().contains(email)
                )
                .verify();
        }
    }

    @Nested
    @DisplayName("Password Encoding Tests")
    class PasswordEncodingTests {

        @Test
        @DisplayName("Should encode password before saving")
        void shouldEncodePasswordBeforeSaving() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String encodedPassword = "EncodedPass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(false));
            when(passwordEncoderGateway.encode(anyString())).thenReturn(encodedPassword);
            when(userGateway.save(any(User.class))).thenReturn(Mono.just(new User(
                new UserId(1L),
                new UserName(name),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            )));

            StepVerifier.create(registerUserUseCase.execute(command))
                .expectNextCount(1)
                .verifyComplete();

            verify(passwordEncoderGateway).encode(password);
            verify(userGateway).save(argThat(user -> 
                user.getPassword().getValue().equals(encodedPassword)
            ));
        }
    }

    @Nested
    @DisplayName("Gateway Interaction Tests")
    class GatewayInteractionTests {

        @Test
        @DisplayName("Should call gateways in correct order")
        void shouldCallGatewaysInCorrectOrder() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String encodedPassword = "EncodedPass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(false));
            when(passwordEncoderGateway.encode(anyString())).thenReturn(encodedPassword);
            when(userGateway.save(any(User.class))).thenReturn(Mono.just(new User(
                new UserId(1L),
                new UserName(name),
                new Email(email),
                new Password(encodedPassword),
                UserRole.STUDENT
            )));

            StepVerifier.create(registerUserUseCase.execute(command))
                .expectNextCount(1)
                .verifyComplete();

            verify(userGateway).existsByEmail(any(Email.class));
            verify(passwordEncoderGateway).encode(password);
            verify(userGateway).save(any(User.class));
        }

        @Test
        @DisplayName("Should not call save when user already exists")
        void shouldNotCallSaveWhenUserAlreadyExists() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            when(userGateway.existsByEmail(any(Email.class))).thenReturn(Mono.just(true));

            StepVerifier.create(registerUserUseCase.execute(command))
                .expectError(UserAlreadyExistsException.class)
                .verify();

            verify(userGateway).existsByEmail(any(Email.class));
            verifyNoMoreInteractions(userGateway);
            verifyNoInteractions(passwordEncoderGateway);
        }
    }
}
