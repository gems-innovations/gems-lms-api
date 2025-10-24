package com.gems.auth.infrastructure.driving.rest.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginRequest Tests")
class LoginRequestTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create login request with default constructor")
        void shouldCreateLoginRequestWithDefaultConstructor() {
            LoginRequest request = new LoginRequest();

            assertNotNull(request);
            assertNull(request.getEmail());
            assertNull(request.getPassword());
        }

        @Test
        @DisplayName("Should create login request with parameters")
        void shouldCreateLoginRequestWithParameters() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            LoginRequest request = new LoginRequest(email, password);

            assertNotNull(request);
            assertEquals(email, request.getEmail());
            assertEquals(password, request.getPassword());
        }

        @Test
        @DisplayName("Should create login request with null values")
        void shouldCreateLoginRequestWithNullValues() {
            LoginRequest request = new LoginRequest(null, null);

            assertNotNull(request);
            assertNull(request.getEmail());
            assertNull(request.getPassword());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterAndSetterTests {

        @Test
        @DisplayName("Should get and set email correctly")
        void shouldGetAndSetEmailCorrectly() {
            LoginRequest request = new LoginRequest();
            String email = "john.doe@example.com";

            request.setEmail(email);

            assertEquals(email, request.getEmail());
        }

        @Test
        @DisplayName("Should get and set password correctly")
        void shouldGetAndSetPasswordCorrectly() {
            LoginRequest request = new LoginRequest();
            String password = "SecurePass123!";

            request.setPassword(password);

            assertEquals(password, request.getPassword());
        }

        @Test
        @DisplayName("Should handle null email")
        void shouldHandleNullEmail() {
            LoginRequest request = new LoginRequest();

            request.setEmail(null);

            assertNull(request.getEmail());
        }

        @Test
        @DisplayName("Should handle null password")
        void shouldHandleNullPassword() {
            LoginRequest request = new LoginRequest();

            request.setPassword(null);

            assertNull(request.getPassword());
        }

        @Test
        @DisplayName("Should handle empty strings")
        void shouldHandleEmptyStrings() {
            LoginRequest request = new LoginRequest();

            request.setEmail("");
            request.setPassword("");

            assertEquals("", request.getEmail());
            assertEquals("", request.getPassword());
        }

        @Test
        @DisplayName("Should handle whitespace strings")
        void shouldHandleWhitespaceStrings() {
            LoginRequest request = new LoginRequest();
            String email = "  john.doe@example.com  ";
            String password = "  SecurePass123!  ";

            request.setEmail(email);
            request.setPassword(password);

            assertEquals(email, request.getEmail());
            assertEquals(password, request.getPassword());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            LoginRequest request = new LoginRequest();
            String email = "user+tag@example-domain.com";

            request.setEmail(email);

            assertEquals(email, request.getEmail());
        }

        @Test
        @DisplayName("Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            LoginRequest request = new LoginRequest();
            String password = "SecurePass123!@#$%^&*()";

            request.setPassword(password);

            assertEquals(password, request.getPassword());
        }

        @Test
        @DisplayName("Should handle very long strings")
        void shouldHandleVeryLongStrings() {
            LoginRequest request = new LoginRequest();
            String longEmail = "a".repeat(100) + "@example.com";
            String longPassword = "a".repeat(100);

            request.setEmail(longEmail);
            request.setPassword(longPassword);

            assertEquals(longEmail, request.getEmail());
            assertEquals(longPassword, request.getPassword());
        }
    }
}
