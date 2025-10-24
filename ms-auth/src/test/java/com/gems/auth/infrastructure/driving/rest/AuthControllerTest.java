package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.LoginUseCase;
import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.domain.exceptions.InvalidCredentialsException;
import com.gems.auth.domain.exceptions.UserNotFoundException;
import com.gems.auth.domain.values.UserRole;
import com.gems.auth.infrastructure.driving.rest.request.LoginRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private LoginUseCase loginUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        AuthController authController = new AuthController(loginUseCase);
        webTestClient = WebTestClient.bindToController(authController).build();
    }

    @Nested
    @DisplayName("Successful Login Tests")
    class SuccessfulLoginTests {

        @Test
        @DisplayName("Should return 200 OK with login response when login is successful")
        void shouldReturn200OkWithLoginResponseWhenLoginIsSuccessful() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            Long userId = 1L;
            String name = "John Doe";
            String token = "jwt-token";

            LoginResponse loginResponse = new LoginResponse(
                userId, name, email, UserRole.STUDENT, token
            );

            when(loginUseCase.execute(any(LoginCommand.class)))
                .thenReturn(Mono.just(loginResponse));

            webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.userId").isEqualTo(userId)
                .jsonPath("$.name").isEqualTo(name)
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.role").isEqualTo("STUDENT")
                .jsonPath("$.token").isEqualTo(token);

            verify(loginUseCase).execute(any(LoginCommand.class));
        }

        @Test
        @DisplayName("Should return 200 OK for teacher login")
        void shouldReturn200OkForTeacherLogin() {
            String email = "teacher@example.com";
            String password = "TeacherPass123!";
            Long userId = 2L;
            String name = "Jane Teacher";
            String token = "jwt-token";

            LoginResponse loginResponse = new LoginResponse(
                userId, name, email, UserRole.TEACHER, token
            );

            when(loginUseCase.execute(any(LoginCommand.class)))
                .thenReturn(Mono.just(loginResponse));

            webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.role").isEqualTo("TEACHER");
        }
    }

    @Nested
    @DisplayName("Invalid Credentials Tests")
    class InvalidCredentialsTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when credentials are invalid")
        void shouldReturn401UnauthorizedWhenCredentialsAreInvalid() {
            String email = "john.doe@example.com";
            String password = "WrongPassword123!";

            when(loginUseCase.execute(any(LoginCommand.class)))
                .thenReturn(Mono.error(new InvalidCredentialsException("Invalid credentials")));

            webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isUnauthorized();

            verify(loginUseCase).execute(any(LoginCommand.class));
        }
    }

    @Nested
    @DisplayName("User Not Found Tests")
    class UserNotFoundTests {

        @Test
        @DisplayName("Should return 404 Not Found when user does not exist")
        void shouldReturn404NotFoundWhenUserDoesNotExist() {
            String email = "nonexistent@example.com";
            String password = "SecurePass123!";

            when(loginUseCase.execute(any(LoginCommand.class)))
                .thenReturn(Mono.error(new UserNotFoundException("User not found")));

            webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isNotFound();

            verify(loginUseCase).execute(any(LoginCommand.class));
        }
    }

    @Nested
    @DisplayName("Internal Server Error Tests")
    class InternalServerErrorTests {

        @Test
        @DisplayName("Should return 500 Internal Server Error for unexpected exceptions")
        void shouldReturn500InternalServerErrorForUnexpectedExceptions() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            when(loginUseCase.execute(any(LoginCommand.class)))
                .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

            webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, password))
                .exchange()
                .expectStatus().is5xxServerError();

            verify(loginUseCase).execute(any(LoginCommand.class));
        }
    }

    @Nested
    @DisplayName("Request Mapping Tests")
    class RequestMappingTests {

        @Test
        @DisplayName("Should map login request to command correctly")
        void shouldMapLoginRequestToCommandCorrectly() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            when(loginUseCase.execute(any(LoginCommand.class)))
                .thenReturn(Mono.just(new LoginResponse(
                    1L, "John Doe", email, UserRole.STUDENT, "token"
                )));

            webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isOk();

            verify(loginUseCase).execute(argThat(command -> 
                command.getEmail().getValue().equals(email) &&
                command.getPassword().getValue().equals(password)
            ));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle multiple error types correctly")
        void shouldHandleMultipleErrorTypesCorrectly() {
            String email = "test@example.com";
            String password = "SecurePass123!";

            when(loginUseCase.execute(any(LoginCommand.class)))
                .thenReturn(Mono.error(new InvalidCredentialsException("Invalid credentials")));

            webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(email, password))
                .exchange()
                .expectStatus().isUnauthorized();

            verify(loginUseCase).execute(any(LoginCommand.class));
        }
    }
}
