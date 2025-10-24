package com.gems.auth.domain.values;

import com.gems.auth.domain.constants.UserConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Password Value Object Tests")
class PasswordTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create Password with valid password")
        void shouldCreatePasswordWithValidPassword() {
            String passwordValue = "SecurePass123!";
            Password password = new Password(passwordValue);

            assertNotNull(password);
            assertEquals("SecurePass123!", password.getValue());
        }

        @Test
        @DisplayName("Should throw exception when password is null")
        void shouldThrowExceptionWhenPasswordIsNull() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Password(null)
            );

            assertEquals(UserConstants.PASSWORD_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when password is empty")
        void shouldThrowExceptionWhenPasswordIsEmpty() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Password("")
            );

            assertEquals(UserConstants.PASSWORD_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when password is only whitespace")
        void shouldThrowExceptionWhenPasswordIsOnlyWhitespace() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Password("   ")
            );

            assertEquals(UserConstants.PASSWORD_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when password is too short")
        void shouldThrowExceptionWhenPasswordIsTooShort() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Password("Pass1!")
            );

            assertEquals(UserConstants.PASSWORD_MIN_LENGTH, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when password doesn't match pattern")
        void shouldThrowExceptionWhenPasswordDoesntMatchPattern() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Password("password123")
            );

            assertEquals(UserConstants.PASSWORD_PATTERN, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Valid Password Pattern Tests")
    class ValidPasswordPatternTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "SecurePass123!",
            "MyPassword1@",
            "Password1$",
            "ValidPass2%",
            "GoodPass3&",
            "StrongPass4*",
            "ComplexPass5?",
            "ValidPass6@",
            "TestPass7!"
        })
        @DisplayName("Should accept valid password patterns")
        void shouldAcceptValidPasswordPatterns(String passwordValue) {
            assertDoesNotThrow(() -> new Password(passwordValue));
        }
    }

    @Nested
    @DisplayName("Invalid Password Pattern Tests")
    class InvalidPasswordPatternTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "password123",
            "PASSWORD123",
            "Password",
            "12345678",
            "Password!",
            "password!",
            "PASSWORD!",
            "12345678!",
            "Pass1",
            "Pass1!",
            "Password1",
            "Password!",
            "PASSWORD1!",
            "password1!"
        })
        @DisplayName("Should reject invalid password patterns")
        void shouldRejectInvalidPasswordPatterns(String passwordValue) {
            assertThrows(IllegalArgumentException.class, () -> new Password(passwordValue));
        }
    }

    @Nested
    @DisplayName("Password Length Tests")
    class PasswordLengthTests {

        @Test
        @DisplayName("Should accept password with minimum length")
        void shouldAcceptPasswordWithMinimumLength() {
            String passwordValue = "Pass123!";
            assertDoesNotThrow(() -> new Password(passwordValue));
        }

        @Test
        @DisplayName("Should accept password with more than minimum length")
        void shouldAcceptPasswordWithMoreThanMinimumLength() {
            String passwordValue = "VeryLongPassword123!@#$";
            assertDoesNotThrow(() -> new Password(passwordValue));
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return original password value")
        void shouldReturnOriginalPasswordValue() {
            String passwordValue = "SecurePass123!";
            Password password = new Password(passwordValue);

            assertEquals(passwordValue, password.getValue());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when passwords are same")
        void shouldBeEqualWhenPasswordsAreSame() {
            Password password1 = new Password("SecurePass123!");
            Password password2 = new Password("SecurePass123!");

            assertEquals(password1, password2);
            assertEquals(password1.hashCode(), password2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when passwords are different")
        void shouldNotBeEqualWhenPasswordsAreDifferent() {
            Password password1 = new Password("SecurePass123!");
            Password password2 = new Password("DifferentPass456!");

            assertNotEquals(password1, password2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            Password password = new Password("SecurePass123!");

            assertEquals(password, password);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            Password password = new Password("SecurePass123!");

            assertNotEquals(password, null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Password password = new Password("SecurePass123!");
            String stringValue = "SecurePass123!";

            assertNotEquals(password, stringValue);
        }
    }
}
