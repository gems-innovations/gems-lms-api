package com.gems.auth.domain.values;

import com.gems.auth.domain.constants.UserConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserName Value Object Tests")
class UserNameTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create UserName with valid name")
        void shouldCreateUserNameWithValidName() {
            String nameValue = "John Doe";
            UserName userName = new UserName(nameValue);

            assertNotNull(userName);
            assertEquals("John Doe", userName.getValue());
        }

        @Test
        @DisplayName("Should trim whitespace from name")
        void shouldTrimWhitespaceFromName() {
            String nameValue = "  John Doe  ";
            UserName userName = new UserName(nameValue);

            assertEquals("John Doe", userName.getValue());
        }

        @Test
        @DisplayName("Should throw exception when name is null")
        void shouldThrowExceptionWhenNameIsNull() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserName(null)
            );

            assertEquals(UserConstants.USER_NAME_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when name is empty")
        void shouldThrowExceptionWhenNameIsEmpty() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserName("")
            );

            assertEquals(UserConstants.USER_NAME_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when name is only whitespace")
        void shouldThrowExceptionWhenNameIsOnlyWhitespace() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserName("   ")
            );

            assertEquals(UserConstants.USER_NAME_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when name is too short")
        void shouldThrowExceptionWhenNameIsTooShort() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserName("J")
            );

            assertEquals(UserConstants.USER_NAME_MIN_LENGTH, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when name is too long")
        void shouldThrowExceptionWhenNameIsTooLong() {
            String longName = "A".repeat(51);
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserName(longName)
            );

            assertEquals(UserConstants.USER_NAME_MAX_LENGTH, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Valid Name Length Tests")
    class ValidNameLengthTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "Jo",
            "John",
            "John Doe",
            "John Michael Doe",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        })
        @DisplayName("Should accept valid name lengths")
        void shouldAcceptValidNameLengths(String nameValue) {
            assertDoesNotThrow(() -> new UserName(nameValue));
        }
    }

    @Nested
    @DisplayName("Invalid Name Length Tests")
    class InvalidNameLengthTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "J",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        })
        @DisplayName("Should reject invalid name lengths")
        void shouldRejectInvalidNameLengths(String nameValue) {
            assertThrows(IllegalArgumentException.class, () -> new UserName(nameValue));
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return trimmed name value")
        void shouldReturnTrimmedNameValue() {
            String nameValue = "  John Doe  ";
            UserName userName = new UserName(nameValue);

            assertEquals("John Doe", userName.getValue());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when names are same")
        void shouldBeEqualWhenNamesAreSame() {
            UserName userName1 = new UserName("John Doe");
            UserName userName2 = new UserName("John Doe");

            assertEquals(userName1, userName2);
            assertEquals(userName1.hashCode(), userName2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when names are same but with different whitespace")
        void shouldBeEqualWhenNamesAreSameButWithDifferentWhitespace() {
            UserName userName1 = new UserName("  John Doe  ");
            UserName userName2 = new UserName("John Doe");

            assertEquals(userName1, userName2);
            assertEquals(userName1.hashCode(), userName2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when names are different")
        void shouldNotBeEqualWhenNamesAreDifferent() {
            UserName userName1 = new UserName("John Doe");
            UserName userName2 = new UserName("Jane Doe");

            assertNotEquals(userName1, userName2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            UserName userName = new UserName("John Doe");

            assertEquals(userName, userName);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            UserName userName = new UserName("John Doe");

            assertNotEquals(userName, null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            UserName userName = new UserName("John Doe");
            String stringValue = "John Doe";

            assertNotEquals(userName, stringValue);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return trimmed name value")
        void shouldReturnTrimmedNameValue() {
            String nameValue = "  John Doe  ";
            UserName userName = new UserName(nameValue);

            assertEquals("John Doe", userName.toString());
        }
    }
}
