package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.DisableUserUseCase;
import com.gems.auth.application.GetAllUsersUseCase;
import com.gems.auth.application.GetUserByIdUseCase;
import com.gems.auth.application.GetUsersByInstitutionUseCase;
import com.gems.auth.application.ToggleUserStatusUseCase;
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

import com.gems.shared.security.AuthenticatedUser;
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
    @Mock
    private ToggleUserStatusUseCase toggleUserStatusUseCase;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(
            disableUserUseCase,
            getUsersByInstitutionUseCase,
            getUserByIdUseCase,
            getAllUsersUseCase,
            updateUserUseCase,
            toggleUserStatusUseCase
        );
        webTestClient = WebTestClient.bindToController(userController)
            .webFilter(TestSecurity.superAdmin())
            .controllerAdvice(new GlobalExceptionHandler(), new com.gems.shared.security.SecurityExceptionAdvice())
            .build();
        // Write endpoints first load the target user to check permissions.
        lenient().when(getUserByIdUseCase.execute(org.mockito.ArgumentMatchers.anyLong())).thenReturn(Mono.just(
            new UserResponse(1L, "John", "Doe", "john.doe", "john@example.com", "STUDENT", "inst-123", null,
                LocalDateTime.now(), LocalDateTime.now(), true)));
    }

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return all users successfully")
        void shouldReturnAllUsersSuccessfully() {
            UserResponse user1 = new UserResponse(1L, "User", "One", "user.one", "one@example.com", "STUDENT", "inst-123", null, LocalDateTime.now(), LocalDateTime.now(), true);
            UserResponse user2 = new UserResponse(2L, "User", "Two", "user.two", "two@example.com", "INSTRUCTOR", "inst-123", null, LocalDateTime.now(), LocalDateTime.now(), true);

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
            UserResponse user = new UserResponse(1L, "John", "Doe", "john.doe", "john@example.com", "STUDENT", "inst-123", null, LocalDateTime.now(), LocalDateTime.now(), true);

            when(getUserByIdUseCase.execute(1L)).thenReturn(Mono.just(user));

            webTestClient.get()
                .uri("/api/v1/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.userId").isEqualTo(1)
                .jsonPath("$.firstName").isEqualTo("John")
                .jsonPath("$.lastName").isEqualTo("Doe");

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
            UserResponse updatedResponse = new UserResponse(1L, "John", "Updated", "john.updated", "john@example.com", "ADMIN", "inst-456", null, LocalDateTime.now(), LocalDateTime.now(), true);

            when(updateUserUseCase.execute(any(UpdateUserCommand.class))).thenReturn(Mono.just(updatedResponse));

            webTestClient.put()
                .uri("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateUserRequest("John", "Updated", "john.updated", "ADMIN", "inst-456"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstName").isEqualTo("John")
                .jsonPath("$.lastName").isEqualTo("Updated")
                .jsonPath("$.role").isEqualTo("ADMIN");

            verify(updateUserUseCase).execute(any(UpdateUserCommand.class));
        }
    }

    @Nested
    @DisplayName("Toggle User Status Tests")
    class ToggleUserStatusTests {

        @Test
        @DisplayName("Should toggle user status successfully")
        void shouldToggleUserStatusSuccessfully() {
            UserResponse toggledResponse = new UserResponse(1L, "John", "Doe", "john.doe", "john@example.com", "STUDENT", "inst-123", null, LocalDateTime.now(), LocalDateTime.now(), false);

            when(toggleUserStatusUseCase.execute(any(UserId.class))).thenReturn(Mono.just(toggledResponse));

            webTestClient.patch()
                .uri("/api/v1/users/1/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.active").isEqualTo(false);

            verify(toggleUserStatusUseCase).execute(any(UserId.class));
        }

        @Test
        @DisplayName("Should return 404 when toggling status for non-existent user")
        void shouldReturn404WhenUserNotFound() {
            when(toggleUserStatusUseCase.execute(any(UserId.class)))
                .thenReturn(Mono.error(new UserNotFoundException("User not found")));

            webTestClient.patch()
                .uri("/api/v1/users/99/status")
                .exchange()
                .expectStatus().isNotFound();
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
            UserResponse user1 = new UserResponse(1L, "User", "One", "user.one", "one@example.com", "STUDENT", institutionId, null, LocalDateTime.now(), LocalDateTime.now(), true);
            UserResponse user2 = new UserResponse(2L, "User", "Two", "user.two", "two@example.com", "INSTRUCTOR", institutionId, null, LocalDateTime.now(), LocalDateTime.now(), true);

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
    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        private WebTestClient as(AuthenticatedUser caller) {
            UserController controller = new UserController(disableUserUseCase, getUsersByInstitutionUseCase,
                getUserByIdUseCase, getAllUsersUseCase, updateUserUseCase, toggleUserStatusUseCase);
            return WebTestClient.bindToController(controller)
                .webFilter(TestSecurity.authenticatedAs(caller))
                .controllerAdvice(new GlobalExceptionHandler(), new com.gems.shared.security.SecurityExceptionAdvice())
                .build();
        }

        @Test
        @DisplayName("User counts: super admin sees every institution, staff only theirs, students none")
        void userCountsAreScopedByRole() {
            when(getAllUsersUseCase.countByInstitution())
                .thenReturn(Mono.just(java.util.Map.of("inst-123", 4L, "inst-999", 2L)));

            as(new AuthenticatedUser(1L, "SUPER_ADMIN", null)).get().uri("/api/v1/users/counts")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$['inst-123']").isEqualTo(4).jsonPath("$['inst-999']").isEqualTo(2);
            as(new AuthenticatedUser(2L, "ADMIN", "inst-123")).get().uri("/api/v1/users/counts")
                .exchange().expectStatus().isOk()
                .expectBody().jsonPath("$['inst-123']").isEqualTo(4).jsonPath("$['inst-999']").doesNotExist();
            as(new AuthenticatedUser(3L, "STUDENT", "inst-123")).get().uri("/api/v1/users/counts")
                .exchange().expectStatus().isForbidden();
        }

        @Test
        @DisplayName("Admin cannot list every user")
        void adminCannotListAllUsers() {
            as(new AuthenticatedUser(2L, "ADMIN", "inst-123")).get().uri("/api/v1/users")
                .exchange().expectStatus().isForbidden();
            verifyNoInteractions(getAllUsersUseCase);
        }

        @Test
        @DisplayName("Admin cannot disable users of another institution")
        void adminCannotDisableOtherInstitutionUser() {
            as(new AuthenticatedUser(2L, "ADMIN", "inst-999")).delete().uri("/api/v1/users/1")
                .exchange().expectStatus().isForbidden();
            verifyNoInteractions(disableUserUseCase);
        }

        @Test
        @DisplayName("Student cannot list institution users")
        void studentCannotListInstitutionUsers() {
            as(new AuthenticatedUser(3L, "STUDENT", "inst-123")).get().uri("/api/v1/users/institution/inst-123")
                .exchange().expectStatus().isForbidden();
        }

        @Test
        @DisplayName("Admin can disable users of their institution")
        void adminCanDisableOwnInstitutionUser() {
            when(disableUserUseCase.execute(any(UserId.class))).thenReturn(Mono.empty());
            as(new AuthenticatedUser(2L, "ADMIN", "inst-123")).delete().uri("/api/v1/users/1")
                .exchange().expectStatus().isNoContent();
        }
    }
}
