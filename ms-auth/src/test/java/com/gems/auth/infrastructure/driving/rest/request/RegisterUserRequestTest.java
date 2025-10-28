package com.gems.auth.infrastructure.driving.rest.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RegisterUserRequest Tests")
class RegisterUserRequestTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create register user request with default constructor")
        void shouldCreateRegisterUserRequestWithDefaultConstructor() {
            RegisterUserRequest request = new RegisterUserRequest();

            assertNotNull(request);
            assertNull(request.getName());
            assertNull(request.getEmail());
            assertNull(request.getPassword());
            assertNull(request.getRole());
        }

        @Test
        @DisplayName("Should create register user request with parameters")
        void shouldCreateRegisterUserRequestWithParameters() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String role = "STUDENT";

            RegisterUserRequest request = new RegisterUserRequest(name, email, password, role);

            assertNotNull(request);
            assertEquals(name, request.getName());
            assertEquals(email, request.getEmail());
            assertEquals(password, request.getPassword());
            assertEquals(role, request.getRole());
        }

        @Test
        @DisplayName("Should create register user request with null values")
        void shouldCreateRegisterUserRequestWithNullValues() {
            RegisterUserRequest request = new RegisterUserRequest(null, null, null, null);

            assertNotNull(request);
            assertNull(request.getName());
            assertNull(request.getEmail());
            assertNull(request.getPassword());
            assertNull(request.getRole());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterAndSetterTests {

        @Test
        @DisplayName("Should get and set name correctly")
        void shouldGetAndSetNameCorrectly() {
            RegisterUserRequest request = new RegisterUserRequest();
            String name = "John Doe";

            request.setName(name);

            assertEquals(name, request.getName());
        }

        @Test
        @DisplayName("Should get and set email correctly")
        void shouldGetAndSetEmailCorrectly() {
            RegisterUserRequest request = new RegisterUserRequest();
            String email = "john.doe@example.com";

            request.setEmail(email);

            assertEquals(email, request.getEmail());
        }

        @Test
        @DisplayName("Should get and set password correctly")
        void shouldGetAndSetPasswordCorrectly() {
            RegisterUserRequest request = new RegisterUserRequest();
            String password = "SecurePass123!";

            request.setPassword(password);

            assertEquals(password, request.getPassword());
        }

        @Test
        @DisplayName("Should get and set role correctly")
        void shouldGetAndSetRoleCorrectly() {
            RegisterUserRequest request = new RegisterUserRequest();
            String role = "TEACHER";

            request.setRole(role);

            assertEquals(role, request.getRole());
        }

        @Test
        @DisplayName("Should handle null name")
        void shouldHandleNullName() {
            RegisterUserRequest request = new RegisterUserRequest();

            request.setName(null);

            assertNull(request.getName());
        }

        @Test
        @DisplayName("Should handle null email")
        void shouldHandleNullEmail() {
            RegisterUserRequest request = new RegisterUserRequest();

            request.setEmail(null);

            assertNull(request.getEmail());
        }

        @Test
        @DisplayName("Should handle null password")
        void shouldHandleNullPassword() {
            RegisterUserRequest request = new RegisterUserRequest();

            request.setPassword(null);

            assertNull(request.getPassword());
        }

        @Test
        @DisplayName("Should handle empty strings")
        void shouldHandleEmptyStrings() {
            RegisterUserRequest request = new RegisterUserRequest();

            request.setName("");
            request.setEmail("");
            request.setPassword("");
            request.setRole("");

            assertEquals("", request.getName());
            assertEquals("", request.getEmail());
            assertEquals("", request.getPassword());
            assertEquals("", request.getRole());
        }

        @Test
        @DisplayName("Should handle whitespace strings")
        void shouldHandleWhitespaceStrings() {
            RegisterUserRequest request = new RegisterUserRequest();
            String name = "  John Doe  ";
            String email = "  john.doe@example.com  ";
            String password = "  SecurePass123!  ";

            request.setName(name);
            request.setEmail(email);
            request.setPassword(password);

            assertEquals(name, request.getName());
            assertEquals(email, request.getEmail());
            assertEquals(password, request.getPassword());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle special characters in name")
        void shouldHandleSpecialCharactersInName() {
            RegisterUserRequest request = new RegisterUserRequest();
            String name = "José María O'Connor-Smith";

            request.setName(name);

            assertEquals(name, request.getName());
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            RegisterUserRequest request = new RegisterUserRequest();
            String email = "user+tag@example-domain.com";

            request.setEmail(email);

            assertEquals(email, request.getEmail());
        }

        @Test
        @DisplayName("Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            RegisterUserRequest request = new RegisterUserRequest();
            String password = "SecurePass123!@#$%^&*()";

            request.setPassword(password);

            assertEquals(password, request.getPassword());
        }

        @Test
        @DisplayName("Should handle very long strings")
        void shouldHandleVeryLongStrings() {
            RegisterUserRequest request = new RegisterUserRequest();
            String longName = "a".repeat(100);
            String longEmail = "a".repeat(100) + "@example.com";
            String longPassword = "a".repeat(100);

            request.setName(longName);
            request.setEmail(longEmail);
            request.setPassword(longPassword);

            assertEquals(longName, request.getName());
            assertEquals(longEmail, request.getEmail());
            assertEquals(longPassword, request.getPassword());
        }
    }
}
