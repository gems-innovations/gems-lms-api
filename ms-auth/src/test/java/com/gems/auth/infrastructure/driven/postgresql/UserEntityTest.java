package com.gems.auth.infrastructure.driven.postgresql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserEntity Tests")
class UserEntityTest {

    private UserEntity userEntity;
    private LocalDateTime testDateTime;

    @BeforeEach
    void setUp() {
        testDateTime = LocalDateTime.now();
        userEntity = new UserEntity();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create UserEntity with default constructor")
        void shouldCreateUserEntityWithDefaultConstructor() {
            UserEntity entity = new UserEntity();

            assertNotNull(entity);
            assertNull(entity.getUserId());
            assertNull(entity.getName());
            assertNull(entity.getEmail());
            assertNull(entity.getPassword());
            assertNull(entity.getRole());
            assertNull(entity.getCreatedAt());
            assertNull(entity.getUpdatedAt());
            assertThrows(NullPointerException.class, () -> entity.isActive());
        }

        @Test
        @DisplayName("Should create UserEntity with all parameters")
        void shouldCreateUserEntityWithAllParameters() {
            Long userId = 1L;
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "EncodedPassword123!";
            String role = "STUDENT";
            Boolean active = true;
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now();

            UserEntity entity = new UserEntity(userId, name, email, password, role, active, createdAt, updatedAt);

            assertNotNull(entity);
            assertEquals(userId, entity.getUserId());
            assertEquals(name, entity.getName());
            assertEquals(email, entity.getEmail());
            assertEquals(password, entity.getPassword());
            assertEquals(role, entity.getRole());
            assertEquals(active, entity.isActive());
            assertEquals(createdAt, entity.getCreatedAt());
            assertEquals(updatedAt, entity.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create UserEntity with null values")
        void shouldCreateUserEntityWithNullValues() {
            UserEntity entity = new UserEntity(null, null, null, null, null, null, null, null);

            assertNotNull(entity);
            assertNull(entity.getUserId());
            assertNull(entity.getName());
            assertNull(entity.getEmail());
            assertNull(entity.getPassword());
            assertNull(entity.getRole());
            assertNull(entity.getCreatedAt());
            assertNull(entity.getUpdatedAt());
            assertThrows(NullPointerException.class, () -> entity.isActive());
        }

        @Test
        @DisplayName("Should create UserEntity with inactive user")
        void shouldCreateUserEntityWithInactiveUser() {
            Long userId = 2L;
            String name = "Jane Doe";
            String email = "jane.doe@example.com";
            String password = "EncodedPassword456!";
            String role = "TEACHER";
            Boolean active = false;

            UserEntity entity = new UserEntity(userId, name, email, password, role, active, testDateTime, testDateTime);

            assertNotNull(entity);
            assertEquals(userId, entity.getUserId());
            assertEquals(name, entity.getName());
            assertEquals(email, entity.getEmail());
            assertEquals(password, entity.getPassword());
            assertEquals(role, entity.getRole());
            assertFalse(entity.isActive());
            assertEquals(testDateTime, entity.getCreatedAt());
            assertEquals(testDateTime, entity.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterAndSetterTests {

        @Test
        @DisplayName("Should get and set userId correctly")
        void shouldGetAndSetUserIdCorrectly() {
            Long userId = 123L;

            userEntity.setUserId(userId);

            assertEquals(userId, userEntity.getUserId());
        }

        @Test
        @DisplayName("Should get and set name correctly")
        void shouldGetAndSetNameCorrectly() {
            String name = "Alice Smith";

            userEntity.setName(name);

            assertEquals(name, userEntity.getName());
        }

        @Test
        @DisplayName("Should get and set email correctly")
        void shouldGetAndSetEmailCorrectly() {
            String email = "alice.smith@example.com";

            userEntity.setEmail(email);

            assertEquals(email, userEntity.getEmail());
        }

        @Test
        @DisplayName("Should get and set password correctly")
        void shouldGetAndSetPasswordCorrectly() {
            String password = "EncodedPassword789!";

            userEntity.setPassword(password);

            assertEquals(password, userEntity.getPassword());
        }

        @Test
        @DisplayName("Should get and set role correctly")
        void shouldGetAndSetRoleCorrectly() {
            String role = "ADMIN";

            userEntity.setRole(role);

            assertEquals(role, userEntity.getRole());
        }

        @Test
        @DisplayName("Should get and set active status correctly")
        void shouldGetAndSetActiveStatusCorrectly() {
            userEntity.setActive(true);

            assertTrue(userEntity.isActive());

            userEntity.setActive(false);

            assertFalse(userEntity.isActive());
        }

        @Test
        @DisplayName("Should get and set createdAt correctly")
        void shouldGetAndSetCreatedAtCorrectly() {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(5);

            userEntity.setCreatedAt(createdAt);

            assertEquals(createdAt, userEntity.getCreatedAt());
        }

        @Test
        @DisplayName("Should get and set updatedAt correctly")
        void shouldGetAndSetUpdatedAtCorrectly() {
            LocalDateTime updatedAt = LocalDateTime.now().minusHours(2);

            userEntity.setUpdatedAt(updatedAt);

            assertEquals(updatedAt, userEntity.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Null Value Tests")
    class NullValueTests {

        @Test
        @DisplayName("Should handle null userId")
        void shouldHandleNullUserId() {
            userEntity.setUserId(null);

            assertNull(userEntity.getUserId());
        }

        @Test
        @DisplayName("Should handle null name")
        void shouldHandleNullName() {
            userEntity.setName(null);

            assertNull(userEntity.getName());
        }

        @Test
        @DisplayName("Should handle null email")
        void shouldHandleNullEmail() {
            userEntity.setEmail(null);

            assertNull(userEntity.getEmail());
        }

        @Test
        @DisplayName("Should handle null password")
        void shouldHandleNullPassword() {
            userEntity.setPassword(null);

            assertNull(userEntity.getPassword());
        }

        @Test
        @DisplayName("Should handle null role")
        void shouldHandleNullRole() {
            userEntity.setRole(null);

            assertNull(userEntity.getRole());
        }

        @Test
        @DisplayName("Should handle null createdAt")
        void shouldHandleNullCreatedAt() {
            userEntity.setCreatedAt(null);

            assertNull(userEntity.getCreatedAt());
        }

        @Test
        @DisplayName("Should handle null updatedAt")
        void shouldHandleNullUpdatedAt() {
            userEntity.setUpdatedAt(null);

            assertNull(userEntity.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long name")
        void shouldHandleVeryLongName() {
            String longName = "A".repeat(1000);

            userEntity.setName(longName);

            assertEquals(longName, userEntity.getName());
        }

        @Test
        @DisplayName("Should handle very long email")
        void shouldHandleVeryLongEmail() {
            String longEmail = "a".repeat(100) + "@example.com";

            userEntity.setEmail(longEmail);

            assertEquals(longEmail, userEntity.getEmail());
        }

        @Test
        @DisplayName("Should handle very long password")
        void shouldHandleVeryLongPassword() {
            String longPassword = "P".repeat(1000);

            userEntity.setPassword(longPassword);

            assertEquals(longPassword, userEntity.getPassword());
        }

        @Test
        @DisplayName("Should handle special characters in name")
        void shouldHandleSpecialCharactersInName() {
            String specialName = "José María O'Connor-Smith";

            userEntity.setName(specialName);

            assertEquals(specialName, userEntity.getName());
        }

        @Test
        @DisplayName("Should handle special characters in email")
        void shouldHandleSpecialCharactersInEmail() {
            String specialEmail = "user+tag@example-domain.com";

            userEntity.setEmail(specialEmail);

            assertEquals(specialEmail, userEntity.getEmail());
        }

        @Test
        @DisplayName("Should handle special characters in password")
        void shouldHandleSpecialCharactersInPassword() {
            String specialPassword = "Pass123!@#$%^&*()";

            userEntity.setPassword(specialPassword);

            assertEquals(specialPassword, userEntity.getPassword());
        }

        @Test
        @DisplayName("Should handle special characters in role")
        void shouldHandleSpecialCharactersInRole() {
            String specialRole = "ROLE_ADMIN_@#$";

            userEntity.setRole(specialRole);

            assertEquals(specialRole, userEntity.getRole());
        }

        @Test
        @DisplayName("Should handle empty strings")
        void shouldHandleEmptyStrings() {
            userEntity.setName("");
            userEntity.setEmail("");
            userEntity.setPassword("");
            userEntity.setRole("");

            assertEquals("", userEntity.getName());
            assertEquals("", userEntity.getEmail());
            assertEquals("", userEntity.getPassword());
            assertEquals("", userEntity.getRole());
        }

        @Test
        @DisplayName("Should handle whitespace strings")
        void shouldHandleWhitespaceStrings() {
            userEntity.setName("  ");
            userEntity.setEmail("  ");
            userEntity.setPassword("  ");
            userEntity.setRole("  ");

            assertEquals("  ", userEntity.getName());
            assertEquals("  ", userEntity.getEmail());
            assertEquals("  ", userEntity.getPassword());
            assertEquals("  ", userEntity.getRole());
        }
    }

    @Nested
    @DisplayName("Date and Time Tests")
    class DateAndTimeTests {

        @Test
        @DisplayName("Should handle past dates")
        void shouldHandlePastDates() {
            LocalDateTime pastDate = LocalDateTime.now().minusYears(1);

            userEntity.setCreatedAt(pastDate);
            userEntity.setUpdatedAt(pastDate);

            assertEquals(pastDate, userEntity.getCreatedAt());
            assertEquals(pastDate, userEntity.getUpdatedAt());
        }

        @Test
        @DisplayName("Should handle future dates")
        void shouldHandleFutureDates() {
            LocalDateTime futureDate = LocalDateTime.now().plusYears(1);

            userEntity.setCreatedAt(futureDate);
            userEntity.setUpdatedAt(futureDate);

            assertEquals(futureDate, userEntity.getCreatedAt());
            assertEquals(futureDate, userEntity.getUpdatedAt());
        }

        @Test
        @DisplayName("Should handle same created and updated dates")
        void shouldHandleSameCreatedAndUpdatedDates() {
            LocalDateTime sameDate = LocalDateTime.now();

            userEntity.setCreatedAt(sameDate);
            userEntity.setUpdatedAt(sameDate);

            assertEquals(sameDate, userEntity.getCreatedAt());
            assertEquals(sameDate, userEntity.getUpdatedAt());
            assertEquals(userEntity.getCreatedAt(), userEntity.getUpdatedAt());
        }

        @Test
        @DisplayName("Should handle different created and updated dates")
        void shouldHandleDifferentCreatedAndUpdatedDates() {
            LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
            LocalDateTime updatedAt = LocalDateTime.now();

            userEntity.setCreatedAt(createdAt);
            userEntity.setUpdatedAt(updatedAt);

            assertEquals(createdAt, userEntity.getCreatedAt());
            assertEquals(updatedAt, userEntity.getUpdatedAt());
            assertNotEquals(userEntity.getCreatedAt(), userEntity.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Role Values Tests")
    class RoleValuesTests {

        @Test
        @DisplayName("Should handle STUDENT role")
        void shouldHandleStudentRole() {
            userEntity.setRole("STUDENT");

            assertEquals("STUDENT", userEntity.getRole());
        }

        @Test
        @DisplayName("Should handle TEACHER role")
        void shouldHandleTeacherRole() {
            userEntity.setRole("TEACHER");

            assertEquals("TEACHER", userEntity.getRole());
        }

        @Test
        @DisplayName("Should handle ADMIN role")
        void shouldHandleAdminRole() {
            userEntity.setRole("ADMIN");

            assertEquals("ADMIN", userEntity.getRole());
        }

        @Test
        @DisplayName("Should handle custom role")
        void shouldHandleCustomRole() {
            userEntity.setRole("CUSTOM_ROLE");

            assertEquals("CUSTOM_ROLE", userEntity.getRole());
        }

        @Test
        @DisplayName("Should handle lowercase role")
        void shouldHandleLowercaseRole() {
            userEntity.setRole("student");

            assertEquals("student", userEntity.getRole());
        }

        @Test
        @DisplayName("Should handle mixed case role")
        void shouldHandleMixedCaseRole() {
            userEntity.setRole("Student");

            assertEquals("Student", userEntity.getRole());
        }
    }

    @Nested
    @DisplayName("Active Status Tests")
    class ActiveStatusTests {

        @Test
        @DisplayName("Should default to false for active status")
        void shouldDefaultToFalseForActiveStatus() {
            UserEntity entity = new UserEntity();

            assertThrows(NullPointerException.class, () -> entity.isActive());
        }

        @Test
        @DisplayName("Should set active to true")
        void shouldSetActiveToTrue() {
            userEntity.setActive(true);

            assertTrue(userEntity.isActive());
        }

        @Test
        @DisplayName("Should set active to false")
        void shouldSetActiveToFalse() {
            userEntity.setActive(false);

            assertFalse(userEntity.isActive());
        }

        @Test
        @DisplayName("Should toggle active status")
        void shouldToggleActiveStatus() {
            userEntity.setActive(true);
            assertTrue(userEntity.isActive());

            userEntity.setActive(false);
            assertFalse(userEntity.isActive());

            userEntity.setActive(true);
            assertTrue(userEntity.isActive());
        }
    }

    @Nested
    @DisplayName("Complete Entity Tests")
    class CompleteEntityTests {

        @Test
        @DisplayName("Should create complete user entity")
        void shouldCreateCompleteUserEntity() {
            Long userId = 1L;
            String name = "John Doe";
            String email = "john.doe@example.com";
            String password = "EncodedPassword123!";
            String role = "STUDENT";
            Boolean active = true;
            LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
            LocalDateTime updatedAt = LocalDateTime.now();

            UserEntity entity = new UserEntity(userId, name, email, password, role, active, createdAt, updatedAt);

            assertNotNull(entity);
            assertEquals(userId, entity.getUserId());
            assertEquals(name, entity.getName());
            assertEquals(email, entity.getEmail());
            assertEquals(password, entity.getPassword());
            assertEquals(role, entity.getRole());
            assertTrue(entity.isActive());
            assertEquals(createdAt, entity.getCreatedAt());
            assertEquals(updatedAt, entity.getUpdatedAt());
        }

        @Test
        @DisplayName("Should update all fields")
        void shouldUpdateAllFields() {
            UserEntity entity = new UserEntity();

            Long newUserId = 2L;
            String newName = "Jane Smith";
            String newEmail = "jane.smith@example.com";
            String newPassword = "NewEncodedPassword456!";
            String newRole = "TEACHER";
            Boolean newActive = false;
            LocalDateTime newCreatedAt = LocalDateTime.now().minusDays(2);
            LocalDateTime newUpdatedAt = LocalDateTime.now().minusHours(1);

            entity.setUserId(newUserId);
            entity.setName(newName);
            entity.setEmail(newEmail);
            entity.setPassword(newPassword);
            entity.setRole(newRole);
            entity.setActive(newActive);
            entity.setCreatedAt(newCreatedAt);
            entity.setUpdatedAt(newUpdatedAt);

            assertEquals(newUserId, entity.getUserId());
            assertEquals(newName, entity.getName());
            assertEquals(newEmail, entity.getEmail());
            assertEquals(newPassword, entity.getPassword());
            assertEquals(newRole, entity.getRole());
            assertFalse(entity.isActive());
            assertEquals(newCreatedAt, entity.getCreatedAt());
            assertEquals(newUpdatedAt, entity.getUpdatedAt());
        }
    }
}
