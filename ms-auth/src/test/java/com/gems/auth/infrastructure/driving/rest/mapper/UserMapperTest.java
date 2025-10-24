package com.gems.auth.infrastructure.driving.rest.mapper;

import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.infrastructure.driving.rest.request.RegisterUserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserMapper Tests")
class UserMapperTest {

    @Nested
    @DisplayName("ToDomain Tests")
    class ToDomainTests {

        @Test
        @DisplayName("Should map register user request to command correctly")
        void shouldMapRegisterUserRequestToCommandCorrectly() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserRequest request = new RegisterUserRequest(name, email, password);
            RegisterUserCommand command = UserMapper.toDomain(request);

            assertNotNull(command);
            assertEquals(name, command.getName().getValue());
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }

        @Test
        @DisplayName("Should throw exception when handling null name")
        void shouldThrowExceptionWhenHandlingNullName() {
            String email = "john.doe@example.com";
            String password = "SecurePass123!";

            RegisterUserRequest request = new RegisterUserRequest(null, email, password);
            
            assertThrows(IllegalArgumentException.class, () -> UserMapper.toDomain(request));
        }

        @Test
        @DisplayName("Should throw exception when handling null email")
        void shouldThrowExceptionWhenHandlingNullEmail() {
            String name = "John Doe";
            String password = "SecurePass123!";

            RegisterUserRequest request = new RegisterUserRequest(name, null, password);
            
            assertThrows(IllegalArgumentException.class, () -> UserMapper.toDomain(request));
        }

        @Test
        @DisplayName("Should throw exception when handling null password")
        void shouldThrowExceptionWhenHandlingNullPassword() {
            String name = "John Doe";
            String email = "john.doe@example.com";

            RegisterUserRequest request = new RegisterUserRequest(name, email, null);
            
            assertThrows(IllegalArgumentException.class, () -> UserMapper.toDomain(request));
        }

        @Test
        @DisplayName("Should throw exception when handling empty strings")
        void shouldThrowExceptionWhenHandlingEmptyStrings() {
            RegisterUserRequest request = new RegisterUserRequest("", "", "");
            
            assertThrows(IllegalArgumentException.class, () -> UserMapper.toDomain(request));
        }

        @Test
        @DisplayName("Should normalize whitespace strings")
        void shouldNormalizeWhitespaceStrings() {
            String name = "  John Doe  ";
            String email = "  john.doe@example.com  ";
            String password = "  SecurePass123!  ";

            RegisterUserRequest request = new RegisterUserRequest(name, email, password);
            RegisterUserCommand command = UserMapper.toDomain(request);

            assertNotNull(command);
            assertEquals("John Doe", command.getName().getValue());
            assertEquals("john.doe@example.com", command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }
    }

    @Nested
    @DisplayName("Value Object Creation Tests")
    class ValueObjectCreationTests {

        @Test
        @DisplayName("Should create UserName value object")
        void shouldCreateUserNameValueObject() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            RegisterUserRequest request = new RegisterUserRequest(name, email, password);
            RegisterUserCommand command = UserMapper.toDomain(request);

            assertNotNull(command.getName());
            assertEquals(name, command.getName().getValue());
        }

        @Test
        @DisplayName("Should create Email value object")
        void shouldCreateEmailValueObject() {
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "SecurePass123!";
            RegisterUserRequest request = new RegisterUserRequest(name, email, password);
            RegisterUserCommand command = UserMapper.toDomain(request);

            assertNotNull(command.getEmail());
            assertEquals(email, command.getEmail().getValue());
        }

        @Test
        @DisplayName("Should create Password value object")
        void shouldCreatePasswordValueObject() {
            String password = "SecurePass123!";
            RegisterUserRequest request = new RegisterUserRequest("John Doe", "email@example.com", password);
            RegisterUserCommand command = UserMapper.toDomain(request);

            assertNotNull(command.getPassword());
            assertEquals(password, command.getPassword().getValue());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle special characters in name")
        void shouldHandleSpecialCharactersInName() {
            String name = "José María O'Connor-Smith";
            String email = "jose.maria@example.com";
            String password = "SecurePass123!";

            RegisterUserRequest request = new RegisterUserRequest(name, email, password);
            RegisterUserCommand command = UserMapper.toDomain(request);

            assertNotNull(command);
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

            RegisterUserRequest request = new RegisterUserRequest(name, email, password);
            RegisterUserCommand command = UserMapper.toDomain(request);

            assertNotNull(command);
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

            RegisterUserRequest request = new RegisterUserRequest(name, email, password);
            RegisterUserCommand command = UserMapper.toDomain(request);

            assertNotNull(command);
            assertEquals(name, command.getName().getValue());
            assertEquals(email, command.getEmail().getValue());
            assertEquals(password, command.getPassword().getValue());
        }
    }
}
