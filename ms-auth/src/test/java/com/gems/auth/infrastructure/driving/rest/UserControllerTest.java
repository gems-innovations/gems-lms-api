package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.exceptions.UserAlreadyExistsException;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.infrastructure.driving.rest.request.RegisterUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(registerUserUseCase);
        webTestClient = WebTestClient.bindToController(userController).build();
    }

    @Nested
    @DisplayName("Successful Registration Tests")
    class SuccessfulRegistrationTests {

        @Test
        @DisplayName("Should return 201 Created with user response when registration is successful")
        void shouldReturn201CreatedWithUserResponseWhenRegistrationIsSuccessful() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            Long userId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime updatedAt = LocalDateTime.now();

            UserResponse userResponse = new UserResponse(
                userId,
                new UserName(name),
                new Email(email),
                createdAt,
                updatedAt,
                true
            );

            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenReturn(Mono.just(userResponse));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId)
                .jsonPath("$.name").isEqualTo(name)
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.active").isEqualTo(true);

            verify(registerUserUseCase).execute(any(RegisterUserCommand.class));
        }

        @Test
        @DisplayName("Should return 201 Created for valid user data")
        void shouldReturn201CreatedForValidUserData() {
            String name = "Jane Doe";
            String email = "jane.doe@example.com";
            String password = "JanePass123!";

            UserResponse userResponse = new UserResponse(
                2L,
                new UserName(name),
                new Email(email),
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
            );

            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenReturn(Mono.just(userResponse));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
        }
    }

    @Nested
    @DisplayName("User Already Exists Tests")
    class UserAlreadyExistsTests {

        @Test
        @DisplayName("Should return 409 Conflict when user already exists")
        void shouldReturn409ConflictWhenUserAlreadyExists() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenReturn(Mono.error(new UserAlreadyExistsException("User already exists")));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password))
                .exchange()
                .expectStatus().isEqualTo(409);

            verify(registerUserUseCase).execute(any(RegisterUserCommand.class));
        }

        @Test
        @DisplayName("Should return 409 Conflict for duplicate email")
        void shouldReturn409ConflictForDuplicateEmail() {
            String name = "Jane Doe";
            String email = "existing@example.com";
            String password = "JanePass123!";

            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenReturn(Mono.error(new UserAlreadyExistsException("User with email existing@example.com already exists")));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password))
                .exchange()
                .expectStatus().isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("Request Mapping Tests")
    class RequestMappingTests {

        @Test
        @DisplayName("Should map register user request to command correctly")
        void shouldMapRegisterUserRequestToCommandCorrectly() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenReturn(Mono.just(new UserResponse(
                    1L,
                    new UserName(name),
                    new Email(email),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    true
                )));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password))
                .exchange()
                .expectStatus().isCreated();

            verify(registerUserUseCase).execute(argThat(command -> 
                command.getName().getValue().equals(name) &&
                command.getEmail().getValue().equals(email) &&
                command.getPassword().getValue().equals(password)
            ));
        }
    }

    @Nested
    @DisplayName("Response Body Tests")
    class ResponseBodyTests {

        @Test
        @DisplayName("Should return user response with correct data")
        void shouldReturnUserResponseWithCorrectData() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            Long userId = 1L;
            LocalDateTime createdAt = LocalDateTime.now();
            LocalDateTime updatedAt = LocalDateTime.now();

            UserResponse userResponse = new UserResponse(
                userId,
                new UserName(name),
                new Email(email),
                createdAt,
                updatedAt,
                true
            );

            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenReturn(Mono.just(userResponse));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(userId)
                .jsonPath("$.name").isEqualTo(name)
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.active").isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle UserAlreadyExistsException correctly")
        void shouldHandleUserAlreadyExistsExceptionCorrectly() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenReturn(Mono.error(new UserAlreadyExistsException("User already exists")));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password))
                .exchange()
                .expectStatus().isEqualTo(409);
        }
    }
}
