package com.gems.auth.infrastructure.driving.rest.mapper;

import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.infrastructure.driving.rest.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LoginMapper Tests")
class LoginMapperTest {

    @Nested
    @DisplayName("ToCommand Tests")
    class ToCommandTests {

        @Test
        @DisplayName("Should map login request to command correctly")
        void shouldMapLoginRequestToCommandCorrectly() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command);
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should map login request with different email formats")
        void shouldMapLoginRequestWithDifferentEmailFormats() {
            String email = "user+tag@example-domain.com";
            String password = "SecurePass123!";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command);
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should map login request with special characters in password")
        void shouldMapLoginRequestWithSpecialCharactersInPassword() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!@#$%^&*()";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command);
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should handle null email")
        void shouldHandleNullEmail() {
            String password = "SecurePass123!";

            LoginRequest request = new LoginRequest(null, password);
            
            assertThrows(IllegalArgumentException.class, () -> {
                LoginMapper.toCommand(request);
            });
        }

        @Test
        @DisplayName("Should handle null password")
        void shouldHandleNullPassword() {
            String email = "john.doe@example.com";

            LoginRequest request = new LoginRequest(email, null);
            
            assertThrows(IllegalArgumentException.class, () -> {
                LoginMapper.toCommand(request);
            });
        }

        @Test
        @DisplayName("Should handle empty email")
        void shouldHandleEmptyEmail() {
            String password = "SecurePass123!";

            LoginRequest request = new LoginRequest("", password);
            
            assertThrows(IllegalArgumentException.class, () -> {
                LoginMapper.toCommand(request);
            });
        }

        @Test
        @DisplayName("Should handle empty password")
        void shouldHandleEmptyPassword() {
            String email = "john.doe@example.com";

            LoginRequest request = new LoginRequest(email, "");
            
            assertThrows(IllegalArgumentException.class, () -> {
                LoginMapper.toCommand(request);
            });
        }


        @Test
        @DisplayName("Should handle whitespace in password")
        void shouldHandleWhitespaceInPassword() {
            String email = "john.doe@example.com";
            String password = "  SecurePass123!  ";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command);
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }
    }

    @Nested
    @DisplayName("Value Object Creation Tests")
    class ValueObjectCreationTests {

        @Test
        @DisplayName("Should create Email value object")
        void shouldCreateEmailValueObject() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command.getEmail());
            assertEquals(email, command.getEmail().getValue());
        }

        @Test
        @DisplayName("Should create Password value object")
        void shouldCreatePasswordValueObject() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command.getPassword());
            assertEquals(password, command.getPassword().getValue());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long email")
        void shouldHandleVeryLongEmail() {
            String email = "a".repeat(100) + "@example.com";
            String password = "SecurePass123!";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command);
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should handle very long password")
        void shouldHandleVeryLongPassword() {
            String email = "john.doe@example.com";
            String password = "a".repeat(100);

            LoginRequest request = new LoginRequest(email, password);
            
            assertThrows(IllegalArgumentException.class, () -> {
                LoginMapper.toCommand(request);
            });
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            String email = "user+tag@example-domain.com";
            String password = "SecurePass123!";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command);
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!@#$%^&*()";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command);
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }
    }
}