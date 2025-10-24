package com.gems.auth.application.command;

import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginCommand Tests")
class LoginCommandTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create login command with valid parameters")
        void shouldCreateLoginCommandWithValidParameters() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            assertNotNull(command);
            assertNotNull(command.getEmail());
            assertNotNull(command.getPassword());
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should throw exception when creating login command with null values")
        void shouldThrowExceptionWhenCreatingLoginCommandWithNullValues() {
            assertThrows(IllegalArgumentException.class, () -> new LoginCommand(null, null));
        }

        @Test
        @DisplayName("Should throw exception when creating login command with empty strings")
        void shouldThrowExceptionWhenCreatingLoginCommandWithEmptyStrings() {
            assertThrows(IllegalArgumentException.class, () -> new LoginCommand("", ""));
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct email")
        void shouldReturnCorrectEmail() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            assertNotNull(command.getEmail());
            assertEquals(email, command.getEmail().getValue());
        }

        @Test
        @DisplayName("Should return correct password")
        void shouldReturnCorrectPassword() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            assertNotNull(command.getPassword());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should return email as Email value object")
        void shouldReturnEmailAsEmailValueObject() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            assertTrue(command.getEmail() instanceof Email);
        }

        @Test
        @DisplayName("Should return password as Password value object")
        void shouldReturnPasswordAsPasswordValueObject() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            assertTrue(command.getPassword() instanceof Password);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            String email = "user+tag@example-domain.com";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!@#$%^&*()";

            LoginCommand command = new LoginCommand(email, password);

            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should handle very long email")
        void shouldHandleVeryLongEmail() {
            String email = "a".repeat(100) + "@example.com";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should throw exception for very long password")
        void shouldThrowExceptionForVeryLongPassword() {
            String email = "john.doe@example.com";
            String password = "a".repeat(100);

            assertThrows(IllegalArgumentException.class, () -> new LoginCommand(email, password));
        }

        @Test
        @DisplayName("Should normalize whitespace in email")
        void shouldNormalizeWhitespaceInEmail() {
            String email = "  john.doe@example.com  ";
            String password = "SecurePass123!";

            LoginCommand command = new LoginCommand(email, password);

            assertEquals("john.doe@example.com", command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should handle whitespace in password")
        void shouldHandleWhitespaceInPassword() {
            String email = "john.doe@example.com";
            String password = "  SecurePass123!  ";

            LoginCommand command = new LoginCommand(email, password);

            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }
    }
}
