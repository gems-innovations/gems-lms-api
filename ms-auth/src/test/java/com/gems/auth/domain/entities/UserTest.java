package com.gems.auth.domain.entities;

import com.gems.auth.domain.values.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Entity Tests")
class UserTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create user with value objects")
        void shouldCreateUserWithValueObjects() {
            UserId id = new UserId(1L);
            UserName name = new UserName("John Doe");
            Email email = new Email("john.doe@example.com");
            Password password = new Password("SecurePass123!");
            UserRole role = UserRole.STUDENT;

            User user = new User(id, name, email, password, role);

            assertNotNull(user);
            assertEquals(id, user.getId());
            assertEquals(name, user.getName());
            assertEquals(email, user.getEmail());
            assertEquals(password, user.getPassword());
            assertEquals(role, user.getRole());
            assertTrue(user.isActive());
            assertNotNull(user.getCreatedAt());
            assertNotNull(user.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create user with primitive values")
        void shouldCreateUserWithPrimitiveValues() {
            User user = new User(1L, "John Doe", "john.doe@example.com", "SecurePass123!", UserRole.STUDENT);

            assertNotNull(user);
            assertEquals(1L, user.getId().getValue());
            assertEquals("John Doe", user.getName().getValue());
            assertEquals("john.doe@example.com", user.getEmail().getValue());
            assertEquals("SecurePass123!", user.getPassword().getValue());
            assertEquals(UserRole.STUDENT, user.getRole());
            assertTrue(user.isActive());
            assertNotNull(user.getCreatedAt());
            assertNotNull(user.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create user without ID for new users")
        void shouldCreateUserWithoutIdForNewUsers() {
            User user = new User("John Doe", "john.doe@example.com", "SecurePass123!", UserRole.STUDENT);

            assertNotNull(user);
            assertNull(user.getId());
            assertEquals("John Doe", user.getName().getValue());
            assertEquals("john.doe@example.com", user.getEmail().getValue());
            assertEquals("SecurePass123!", user.getPassword().getValue());
            assertEquals(UserRole.STUDENT, user.getRole());
            assertTrue(user.isActive());
            assertNotNull(user.getCreatedAt());
            assertNotNull(user.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct values from getters")
        void shouldReturnCorrectValuesFromGetters() {
            UserId id = new UserId(1L);
            UserName name = new UserName("John Doe");
            Email email = new Email("john.doe@example.com");
            Password password = new Password("SecurePass123!");
            UserRole role = UserRole.TEACHER;

            User user = new User(id, name, email, password, role);

            assertEquals(id, user.getId());
            assertEquals(name, user.getName());
            assertEquals(email, user.getEmail());
            assertEquals(password, user.getPassword());
            assertEquals(role, user.getRole());
            assertTrue(user.isActive());
            assertNotNull(user.getCreatedAt());
            assertNotNull(user.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Timestamp Tests")
    class TimestampTests {

        @Test
        @DisplayName("Should set creation and update timestamps")
        void shouldSetCreationAndUpdateTimestamps() {
            LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
            
            User user = new User("John Doe", "john.doe@example.com", "SecurePass123!", UserRole.STUDENT);
            
            LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);
            
            assertNotNull(user.getCreatedAt());
            assertNotNull(user.getUpdatedAt());
            assertTrue(user.getCreatedAt().isAfter(beforeCreation));
            assertTrue(user.getCreatedAt().isBefore(afterCreation));
            assertTrue(user.getUpdatedAt().isAfter(beforeCreation));
            assertTrue(user.getUpdatedAt().isBefore(afterCreation));
        }
    }

    @Nested
    @DisplayName("Active Status Tests")
    class ActiveStatusTests {

        @Test
        @DisplayName("Should be active by default")
        void shouldBeActiveByDefault() {
            User user = new User("John Doe", "john.doe@example.com", "SecurePass123!", UserRole.STUDENT);
            
            assertTrue(user.isActive());
        }
    }
}
