package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.DisableUserUseCase;
import com.gems.auth.application.GetAllUsersUseCase;
import com.gems.auth.application.GetUserByIdUseCase;
import com.gems.auth.application.GetUsersByInstitutionUseCase;
import com.gems.auth.application.UpdateUserUseCase;
import com.gems.auth.application.command.UpdateUserCommand;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.application.response.UserResponse;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.infrastructure.driving.rest.request.UpdateUserRequest;
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
    private DisableUserUseCase disableUserUseCase;
    @Mock
    private GetUsersByInstitutionUseCase getUsersByInstitutionUseCase;
    @Mock
    private GetUserByIdUseCase getUserByIdUseCase;
    @Mock
    private GetAllUsersUseCase getAllUsersUseCase;
    @Mock
    private UpdateUserUseCase updateUserUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(
            disableUserUseCase,
            getUsersByInstitutionUseCase,
            getUserByIdUseCase,
            getAllUsersUseCase,
            updateUserUseCase
        );
        webTestClient = WebTestClient.bindToController(userController).build();
    }

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return all users successfully")
        void shouldReturnAllUsersSuccessfully() {
            UserResponse user1 = new UserResponse(1L, "User One", "one@example.com", "STUDENT", "inst-123", LocalDateTime.now(), LocalDateTime.now(), true);
            UserResponse user2 = new UserResponse(2L, "User Two", "two@example.com", "TEACHER", "inst-123", LocalDateTime.now(), LocalDateTime.now(), true);

            when(getAllUsersUseCase.execute()).thenReturn(Flux.just(user1, user2));

            webTestClient.get()
                .uri("/api/v1/users")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(UserResponse.class)
                .hasSize(2);

            verify(getAllUsersUseCase).execute();
        }
    }

    @Nested
    @DisplayName("Get User by ID Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when ID exists")
        void shouldReturnUserWhenIdExists() {
            UserResponse user = new UserResponse(1L, "John Doe", "john@example.com", "STUDENT", "inst-123", LocalDateTime.now(), LocalDateTime.now(), true);

            when(getUserByIdUseCase.execute(1L)).thenReturn(Mono.just(user));

            webTestClient.get()
                .uri("/api/v1/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.userId").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("John Doe");

            verify(getUserByIdUseCase).execute(1L);
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() {
            when(getUserByIdUseCase.execute(99L)).thenReturn(Mono.error(new UserNotFoundException("User not found")));

            webTestClient.get()
                .uri("/api/v1/users/99")
                .exchange()
                .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user successfully")
        void shouldUpdateUserSuccessfully() {
            UserResponse updatedResponse = new UserResponse(1L, "John Updated", "john@example.com", "ADMIN", "inst-456", LocalDateTime.now(), LocalDateTime.now(), true);

            when(updateUserUseCase.execute(any(UpdateUserCommand.class))).thenReturn(Mono.just(updatedResponse));

            webTestClient.put()
                .uri("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateUserRequest("John Updated", "ADMIN", "inst-456"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("John Updated")
                .jsonPath("$.role").isEqualTo("ADMIN");

            verify(updateUserUseCase).execute(any(UpdateUserCommand.class));
        }
    }

    @Nested
    @DisplayName("Disable User Tests")
    class DisableUserTests {

        @Test
        @DisplayName("Should disable user successfully")
        void shouldDisableUserSuccessfully() {
            when(disableUserUseCase.execute(any(UserId.class))).thenReturn(Mono.empty());

            webTestClient.delete()
                .uri("/api/v1/users/1")
                .exchange()
                .expectStatus().isNoContent();

            verify(disableUserUseCase).execute(any(UserId.class));
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
