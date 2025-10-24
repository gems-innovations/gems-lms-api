package com.gems.auth.domain.values;

import com.gems.auth.domain.constants.UserConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserId Value Object Tests")
class UserIdTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create UserId with valid value")
        void shouldCreateUserIdWithValidValue() {
            Long value = 1L;
            UserId userId = new UserId(value);

            assertNotNull(userId);
            assertEquals(value, userId.getValue());
        }

        @Test
        @DisplayName("Should throw exception when value is null")
        void shouldThrowExceptionWhenValueIsNull() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserId(null)
            );

            assertEquals(UserConstants.USER_ID_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct value")
        void shouldReturnCorrectValue() {
            Long value = 123L;
            UserId userId = new UserId(value);

            assertEquals(value, userId.getValue());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when values are same")
        void shouldBeEqualWhenValuesAreSame() {
            UserId userId1 = new UserId(1L);
            UserId userId2 = new UserId(1L);

            assertEquals(userId1, userId2);
            assertEquals(userId1.hashCode(), userId2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when values are different")
        void shouldNotBeEqualWhenValuesAreDifferent() {
            UserId userId1 = new UserId(1L);
            UserId userId2 = new UserId(2L);

            assertNotEquals(userId1, userId2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            UserId userId = new UserId(1L);

            assertEquals(userId, userId);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToString() {
            UserId userId = new UserId(1L);

            assertNotEquals(userId, null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            UserId userId = new UserId(1L);
            String stringValue = "1";

            assertNotEquals(userId, stringValue);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return string representation of value")
        void shouldReturnStringRepresentationOfValue() {
            Long value = 123L;
            UserId userId = new UserId(value);

            assertEquals(value.toString(), userId.toString());
        }
    }

    @Nested
    @DisplayName("Generate Method Tests")
    class GenerateMethodTests {

        @Test
        @DisplayName("Should return null for generate method")
        void shouldReturnNullForGenerateMethod() {
            UserId userId = UserId.generate();

            assertNull(userId);
        }
    }
}
