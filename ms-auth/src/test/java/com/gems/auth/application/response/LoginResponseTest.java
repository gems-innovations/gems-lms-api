package com.gems.auth.application.response;

import com.gems.auth.domain.values.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginResponse Tests")
class LoginResponseTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create login response with all parameters")
        void shouldCreateLoginResponseWithAllParameters() {
            Long userId = 1L;
            String name = "John Doe";
            String email = "john.doe@example.com";
            UserRole role = UserRole.STUDENT;
            String token = "jwt-token";

            LoginResponse response = new LoginResponse(userId, name, email, role, token);

            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertEquals(name, response.getName());
            assertEquals(email, response.getEmail());
            assertEquals(role, response.getRole());
            assertEquals(token, response.getToken());
        }

        @Test
        @DisplayName("Should create login response with teacher role")
        void shouldCreateLoginResponseWithTeacherRole() {
            Long userId = 2L;
            String name = "Jane Teacher";
            String email = "jane.teacher@example.com";
            UserRole role = UserRole.TEACHER;
            String token = "jwt-token";

            LoginResponse response = new LoginResponse(userId, name, email, role, token);

            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertEquals(name, response.getName());
            assertEquals(email, response.getEmail());
            assertEquals(role, response.getRole());
            assertEquals(token, response.getToken());
        }

        @Test
        @DisplayName("Should create login response with admin role")
        void shouldCreateLoginResponseWithAdminRole() {
            Long userId = 3L;
            String name = "Admin User";
            String email = "admin@example.com";
            UserRole role = UserRole.ADMIN;
            String token = "jwt-token";

            LoginResponse response = new LoginResponse(userId, name, email, role, token);

            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertEquals(name, response.getName());
            assertEquals(email, response.getEmail());
            assertEquals(role, response.getRole());
            assertEquals(token, response.getToken());
        }

        @Test
        @DisplayName("Should create login response with super admin role")
        void shouldCreateLoginResponseWithSuperAdminRole() {
            Long userId = 4L;
            String name = "Super Admin";
            String email = "superadmin@example.com";
            UserRole role = UserRole.SUPER_ADMIN;
            String token = "jwt-token";

            LoginResponse response = new LoginResponse(userId, name, email, role, token);

            assertNotNull(response);
            assertEquals(userId, response.getUserId());
            assertEquals(name, response.getName());
            assertEquals(email, response.getEmail());
            assertEquals(role, response.getRole());
            assertEquals(token, response.getToken());
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct user ID")
        void shouldReturnCorrectUserId() {
            Long userId = 1L;
            LoginResponse response = new LoginResponse(userId, "John Doe", "john@example.com", UserRole.STUDENT, "token");

            assertEquals(userId, response.getUserId());
        }

        @Test
        @DisplayName("Should return correct name")
        void shouldReturnCorrectName() {
            String name = "John Doe";
            LoginResponse response = new LoginResponse(1L, name, "john@example.com", UserRole.STUDENT, "token");

            assertEquals(name, response.getName());
        }

        @Test
        @DisplayName("Should return correct email")
        void shouldReturnCorrectEmail() {
            String email = "john.doe@example.com";
            LoginResponse response = new LoginResponse(1L, "John Doe", email, UserRole.STUDENT, "token");

            assertEquals(email, response.getEmail());
        }

        @Test
        @DisplayName("Should return correct role")
        void shouldReturnCorrectRole() {
            UserRole role = UserRole.TEACHER;
            LoginResponse response = new LoginResponse(1L, "John Doe", "john@example.com", role, "token");

            assertEquals(role, response.getRole());
        }

        @Test
        @DisplayName("Should return correct token")
        void shouldReturnCorrectToken() {
            String token = "jwt-token-12345";
            LoginResponse response = new LoginResponse(1L, "John Doe", "john@example.com", UserRole.STUDENT, token);

            assertEquals(token, response.getToken());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null values")
        void shouldHandleNullValues() {
            LoginResponse response = new LoginResponse(null, null, null, null, null);

            assertNotNull(response);
            assertNull(response.getUserId());
            assertNull(response.getName());
            assertNull(response.getEmail());
            assertNull(response.getRole());
            assertNull(response.getToken());
        }

        @Test
        @DisplayName("Should handle empty strings")
        void shouldHandleEmptyStrings() {
            String name = "";
            String email = "";
            String token = "";

            LoginResponse response = new LoginResponse(1L, name, email, UserRole.STUDENT, token);

            assertEquals(name, response.getName());
            assertEquals(email, response.getEmail());
            assertEquals(token, response.getToken());
        }

        @Test
        @DisplayName("Should handle special characters in name")
        void shouldHandleSpecialCharactersInName() {
            String name = "José María O'Connor-Smith";
            LoginResponse response = new LoginResponse(1L, name, "john@example.com", UserRole.STUDENT, "token");

            assertEquals(name, response.getName());
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            String email = "user+tag@example-domain.com";
            LoginResponse response = new LoginResponse(1L, "John Doe", email, UserRole.STUDENT, "token");

            assertEquals(email, response.getEmail());
        }

        @Test
        @DisplayName("Should handle long token")
        void shouldHandleLongToken() {
            String token = "a".repeat(1000);
            LoginResponse response = new LoginResponse(1L, "John Doe", "john@example.com", UserRole.STUDENT, token);

            assertEquals(token, response.getToken());
        }
    }
}
