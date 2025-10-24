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
        @DisplayName("Should throw exception when handling null email")
        void shouldThrowExceptionWhenHandlingNullEmail() {
            String password = "SecurePass123!";

            LoginRequest request = new LoginRequest(null, password);
            
            assertThrows(IllegalArgumentException.class, () -> LoginMapper.toCommand(request));
        }

        @Test
        @DisplayName("Should throw exception when handling null password")
        void shouldThrowExceptionWhenHandlingNullPassword() {
            String email = "john.doe@example.com";

            LoginRequest request = new LoginRequest(email, null);
            
            assertThrows(IllegalArgumentException.class, () -> LoginMapper.toCommand(request));
        }

        @Test
        @DisplayName("Should throw exception when handling empty strings")
        void shouldThrowExceptionWhenHandlingEmptyStrings() {
            LoginRequest request = new LoginRequest("", "");
            
            assertThrows(IllegalArgumentException.class, () -> LoginMapper.toCommand(request));
        }

        @Test
        @DisplayName("Should normalize whitespace strings")
        void shouldNormalizeWhitespaceStrings() {
            String email = "  john.doe@example.com  ";
            String password = "  SecurePass123!  ";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command);
            assertEquals("john.doe@example.com", command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }
    }

    @Nested
    @DisplayName("Value Object Creation Tests")
    class ValueObjectCreationTests {

        @Test
        @DisplayName("Should create Email value object")
        void shouldCreateEmailValueObject() {
            String email = "test@example.com";
            String password = "SecurePass123!";
            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command.getEmail());
            assertEquals(email, command.getEmail().getValue());
        }

        @Test
        @DisplayName("Should create Password value object")
        void shouldCreatePasswordValueObject() {
            String password = "SecurePass123!";
            LoginRequest request = new LoginRequest("email@example.com", password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command.getPassword());
            assertEquals(password, command.getPassword().getValue());
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
            String password = "SecurePass123!@#$%";

            LoginRequest request = new LoginRequest(email, password);
            LoginCommand command = LoginMapper.toCommand(request);

            assertNotNull(command);
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }
    }
}
