package com.gems.auth.application.command;

import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RegisterUserCommand Tests")
class RegisterUserCommandTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create register user command with valid parameters")
        void shouldCreateRegisterUserCommandWithValidParameters() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            String role = "STUDENT";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, role);

            assertNotNull(command);
            assertNotNull(command.getName());
            assertNotNull(command.getEmail());
            assertNotNull(command.getPassword());
            assertEquals(name, command.getName().getValue());
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
            assertEquals(UserRole.STUDENT, command.getRole());
        }

        @Test
        @DisplayName("Should throw exception when creating register user command with null values")
        void shouldThrowExceptionWhenCreatingRegisterUserCommandWithNullValues() {
            assertThrows(IllegalArgumentException.class, () -> new RegisterUserCommand(null, null, null, "STUDENT"));
        }

        @Test
        @DisplayName("Should throw exception when creating register user command with empty strings")
        void shouldThrowExceptionWhenCreatingRegisterUserCommandWithEmptyStrings() {
            assertThrows(IllegalArgumentException.class, () -> new RegisterUserCommand("", "", "", "STUDENT"));
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct name")
        void shouldReturnCorrectName() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertNotNull(command.getName());
            assertEquals(name, command.getName().getValue());
        }

        @Test
        @DisplayName("Should return correct email")
        void shouldReturnCorrectEmail() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertNotNull(command.getEmail());
            assertEquals(email, command.getEmail().getValue());
        }

        @Test
        @DisplayName("Should return correct password")
        void shouldReturnCorrectPassword() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertNotNull(command.getPassword());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should return name as UserName value object")
        void shouldReturnNameAsUserNameValueObject() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertTrue(command.getName() instanceof UserName);
        }

        @Test
        @DisplayName("Should return email as Email value object")
        void shouldReturnEmailAsEmailValueObject() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertTrue(command.getEmail() instanceof Email);
        }

        @Test
        @DisplayName("Should return password as Password value object")
        void shouldReturnPasswordAsPasswordValueObject() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertTrue(command.getPassword() instanceof Password);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle special characters in name")
        void shouldHandleSpecialCharactersInName() {
            String name = "José María O'Connor-Smith";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertEquals(name, command.getName().getValue());
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            String name = "John Doe";
            String email = "user+tag@example-domain.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertEquals(name, command.getName().getValue());
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!@#$%^&*()";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertEquals(name, command.getName().getValue());
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should throw exception for very long name")
        void shouldThrowExceptionForVeryLongName() {
            String name = "a".repeat(100);
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            assertThrows(IllegalArgumentException.class, () -> new RegisterUserCommand(name, email, password, "STUDENT"));
        }

        @Test
        @DisplayName("Should handle very long email")
        void shouldHandleVeryLongEmail() {
            String name = "John Doe";
            String email = "a".repeat(100) + "@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertEquals(name, command.getName().getValue());
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should throw exception for very long password")
        void shouldThrowExceptionForVeryLongPassword() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "a".repeat(100);

            assertThrows(IllegalArgumentException.class, () -> new RegisterUserCommand(name, email, password, "STUDENT"));
        }

        @Test
        @DisplayName("Should normalize whitespace in name")
        void shouldNormalizeWhitespaceInName() {
            String name = "  John Doe  ";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertEquals("John Doe", command.getName().getValue());
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should normalize whitespace in email")
        void shouldNormalizeWhitespaceInEmail() {
            String name = "John Doe";
            String email = "  john.doe@example.com  ";
            String password = "SecurePass123!";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertEquals(name, command.getName().getValue());
            assertEquals("john.doe@example.com", command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should handle whitespace in password")
        void shouldHandleWhitespaceInPassword() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "  SecurePass123!  ";

            RegisterUserCommand command = new RegisterUserCommand(name, email, password, "STUDENT");

            assertEquals(name, command.getName().getValue());
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }
    }
}
