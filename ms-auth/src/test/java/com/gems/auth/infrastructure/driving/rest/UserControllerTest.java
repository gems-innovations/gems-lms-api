package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.DisableUserUseCase;
import com.gems.auth.application.LoginUseCase;
import com.gems.auth.application.RegisterUserUseCase;
import com.gems.auth.application.GetUsersByInstitutionUseCase;
import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.application.exceptions.UserAlreadyExistsException;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;
    @Mock
    private DisableUserUseCase disableUserUseCase;
    @Mock
    private LoginUseCase loginUseCase;
    @Mock
    private GetUsersByInstitutionUseCase getUsersByInstitutionUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(registerUserUseCase, loginUseCase, disableUserUseCase, getUsersByInstitutionUseCase);
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
                name,
                email,
                "STUDENT",
                "inst-123",
                createdAt,
                updatedAt,
                true
            );

            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenReturn(Mono.just(userResponse));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password, "STUDENT", "inst-123"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.userId").isEqualTo(userId)
                .jsonPath("$.name").isEqualTo(name)
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.institutionId").isEqualTo("inst-123")
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
                name,
                email,
                "STUDENT",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true
            );

            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenReturn(Mono.just(userResponse));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password, "STUDENT"))
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
                .bodyValue(new RegisterUserRequest(name, email, password, "STUDENT"))
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
                .bodyValue(new RegisterUserRequest(name, email, password, "STUDENT"))
                .exchange()
                .expectStatus().isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("RequestMapping Tests")
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
                    name,
                    email,
                    "STUDENT",
                    "inst-123",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    true
                )));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password, "STUDENT", "inst-123"))
                .exchange()
                .expectStatus().isCreated();

            verify(registerUserUseCase).execute(argThat(command -> 
                command.name().equals(name) &&
                command.email().equals(email) &&
                command.password().equals(password) &&
                "inst-123".equals(command.institutionId())
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
                name,
                email,
                "STUDENT",
                "inst-123",
                createdAt,
                updatedAt,
                true
            );

            when(registerUserUseCase.execute(any(RegisterUserCommand.class)))
                .thenReturn(Mono.just(userResponse));

            webTestClient.post()
                .uri("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterUserRequest(name, email, password, "STUDENT", "inst-123"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.userId").isEqualTo(userId)
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
                .bodyValue(new RegisterUserRequest(name, email, password, "STUDENT"))
                .exchange()
                .expectStatus().isEqualTo(409);
        }
    }

    @Nested
    @DisplayName("Get Users by Institution Tests")
    class GetUsersByInstitutionTests {

        @Test
        @DisplayName("Should return users in institution successfully")
        void shouldReturnUsersInInstitutionSuccessfully() {
            String institutionId = "inst-123";
            UserResponse user1 = new UserResponse(1L, "User One", "one@example.com", "STUDENT", institutionId, LocalDateTime.now(), LocalDateTime.now(), true);
            UserResponse user2 = new UserResponse(2L, "User Two", "two@example.com", "TEACHER", institutionId, LocalDateTime.now(), LocalDateTime.now(), true);

            when(getUsersByInstitutionUseCase.execute(institutionId)).thenReturn(Flux.just(user1, user2));

            webTestClient.get()
                .uri("/api/v1/users/institution/" + institutionId)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(UserResponse.class)
                .hasSize(2);

            verify(getUsersByInstitutionUseCase).execute(institutionId);
        }
    }
}
