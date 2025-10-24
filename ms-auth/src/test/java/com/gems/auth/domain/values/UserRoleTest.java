package com.gems.auth.domain.values;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserRole Enum Tests")
class UserRoleTest {

    @Nested
    @DisplayName("Enum Values Tests")
    class EnumValuesTests {

        @Test
        @DisplayName("Should contain all expected roles")
        void shouldContainAllExpectedRoles() {
            UserRole[] roles = UserRole.values();

            assertEquals(4, roles.length);
            assertTrue(containsRole(roles, UserRole.STUDENT));
            assertTrue(containsRole(roles, UserRole.TEACHER));
            assertTrue(containsRole(roles, UserRole.ADMIN));
            assertTrue(containsRole(roles, UserRole.SUPER_ADMIN));
        }

        @Test
        @DisplayName("Should have correct string representation")
        void shouldHaveCorrectStringRepresentation() {
            assertEquals("STUDENT", UserRole.STUDENT.name());
            assertEquals("TEACHER", UserRole.TEACHER.name());
            assertEquals("ADMIN", UserRole.ADMIN.name());
            assertEquals("SUPER_ADMIN", UserRole.SUPER_ADMIN.name());
        }

        @Test
        @DisplayName("Should have correct ordinal values")
        void shouldHaveCorrectOrdinalValues() {
            assertEquals(0, UserRole.STUDENT.ordinal());
            assertEquals(1, UserRole.TEACHER.ordinal());
            assertEquals(2, UserRole.ADMIN.ordinal());
            assertEquals(3, UserRole.SUPER_ADMIN.ordinal());
        }
    }

    @Nested
    @DisplayName("ValueOf Tests")
    class ValueOfTests {

        @Test
        @DisplayName("Should return correct role from string")
        void shouldReturnCorrectRoleFromString() {
            assertEquals(UserRole.STUDENT, UserRole.valueOf("STUDENT"));
            assertEquals(UserRole.TEACHER, UserRole.valueOf("TEACHER"));
            assertEquals(UserRole.ADMIN, UserRole.valueOf("ADMIN"));
            assertEquals(UserRole.SUPER_ADMIN, UserRole.valueOf("SUPER_ADMIN"));
        }

        @Test
        @DisplayName("Should throw exception for invalid role string")
        void shouldThrowExceptionForInvalidRoleString() {
            assertThrows(IllegalArgumentException.class, () -> UserRole.valueOf("INVALID_ROLE"));
        }

        @Test
        @DisplayName("Should throw exception for null string")
        void shouldThrowExceptionForNullString() {
            assertThrows(NullPointerException.class, () -> UserRole.valueOf(null));
        }
    }

    @Nested
    @DisplayName("Equality Tests")
    class EqualityTests {

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            assertEquals(UserRole.STUDENT, UserRole.STUDENT);
            assertEquals(UserRole.TEACHER, UserRole.TEACHER);
            assertEquals(UserRole.ADMIN, UserRole.ADMIN);
            assertEquals(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN);
        }

        @Test
        @DisplayName("Should not be equal to different roles")
        void shouldNotBeEqualToDifferentRoles() {
            assertNotEquals(UserRole.STUDENT, UserRole.TEACHER);
            assertNotEquals(UserRole.TEACHER, UserRole.ADMIN);
            assertNotEquals(UserRole.ADMIN, UserRole.SUPER_ADMIN);
            assertNotEquals(UserRole.STUDENT, UserRole.SUPER_ADMIN);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return correct string representation")
        void shouldReturnCorrectStringRepresentation() {
            assertEquals("STUDENT", UserRole.STUDENT.toString());
            assertEquals("TEACHER", UserRole.TEACHER.toString());
            assertEquals("ADMIN", UserRole.ADMIN.toString());
            assertEquals("SUPER_ADMIN", UserRole.SUPER_ADMIN.toString());
        }
    }

    @Nested
    @DisplayName("HashCode Tests")
    class HashCodeTests {

        @Test
        @DisplayName("Should have consistent hash codes")
        void shouldHaveConsistentHashCodes() {
            assertEquals(UserRole.STUDENT.hashCode(), UserRole.STUDENT.hashCode());
            assertEquals(UserRole.TEACHER.hashCode(), UserRole.TEACHER.hashCode());
            assertEquals(UserRole.ADMIN.hashCode(), UserRole.ADMIN.hashCode());
            assertEquals(UserRole.SUPER_ADMIN.hashCode(), UserRole.SUPER_ADMIN.hashCode());
        }

        @Test
        @DisplayName("Should have different hash codes for different roles")
        void shouldHaveDifferentHashCodesForDifferentRoles() {
            assertNotEquals(UserRole.STUDENT.hashCode(), UserRole.TEACHER.hashCode());
            assertNotEquals(UserRole.TEACHER.hashCode(), UserRole.ADMIN.hashCode());
            assertNotEquals(UserRole.ADMIN.hashCode(), UserRole.SUPER_ADMIN.hashCode());
        }
    }

    private boolean containsRole(UserRole[] roles, UserRole targetRole) {
        for (UserRole role : roles) {
            if (role == targetRole) {
                return true;
            }
        }
        return false;
    }
}
