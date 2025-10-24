package com.gems.auth.domain.values;

import com.gems.auth.domain.constants.UserConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Email Value Object Tests")
class EmailTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create Email with valid email")
        void shouldCreateEmailWithValidEmail() {
            String emailValue = "john.doe@example.com";
            Email email = new Email(emailValue);

            assertNotNull(email);
            assertEquals("john.doe@example.com", email.getValue());
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void shouldNormalizeEmailToLowercase() {
            String emailValue = "JOHN.DOE@EXAMPLE.COM";
            Email email = new Email(emailValue);

            assertEquals("john.doe@example.com", email.getValue());
        }

        @Test
        @DisplayName("Should trim whitespace from email")
        void shouldTrimWhitespaceFromEmail() {
            String emailValue = "  john.doe@example.com  ";
            Email email = new Email(emailValue);

            assertEquals("john.doe@example.com", email.getValue());
        }

        @Test
        @DisplayName("Should throw exception when email is null")
        void shouldThrowExceptionWhenEmailIsNull() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Email(null)
            );

            assertEquals(UserConstants.EMAIL_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when email is empty")
        void shouldThrowExceptionWhenEmailIsEmpty() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Email("")
            );

            assertEquals(UserConstants.EMAIL_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when email is only whitespace")
        void shouldThrowExceptionWhenEmailIsOnlyWhitespace() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Email("   ")
            );

            assertEquals(UserConstants.EMAIL_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when email format is invalid")
        void shouldThrowExceptionWhenEmailFormatIsInvalid() {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Email("invalid-email")
            );

            assertEquals(UserConstants.INVALID_EMAIL_FORMAT, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Valid Email Format Tests")
    class ValidEmailFormatTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "user@example.com",
            "user.name@example.com",
            "user+tag@example.com",
            "user123@example.com",
            "user@sub.example.com",
            "user@example.co.uk",
            "user@example-domain.com"
        })
        @DisplayName("Should accept valid email formats")
        void shouldAcceptValidEmailFormats(String emailValue) {
            assertDoesNotThrow(() -> new Email(emailValue));
        }
    }

    @Nested
    @DisplayName("Invalid Email Format Tests")
    class InvalidEmailFormatTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "invalid-email",
            "@example.com",
            "user@",
            "user@.com",
            "user@example",
            "user name@example.com",
            "user@example com"
        })
        @DisplayName("Should reject invalid email formats")
        void shouldRejectInvalidEmailFormats(String emailValue) {
            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Email(emailValue)
            );

            assertEquals(UserConstants.INVALID_EMAIL_FORMAT, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return normalized email value")
        void shouldReturnNormalizedEmailValue() {
            String emailValue = "  JOHN.DOE@EXAMPLE.COM  ";
            Email email = new Email(emailValue);

            assertEquals("john.doe@example.com", email.getValue());
        }
    }

    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {

        @Test
        @DisplayName("Should be equal when emails are same")
        void shouldBeEqualWhenEmailsAreSame() {
            Email email1 = new Email("john.doe@example.com");
            Email email2 = new Email("john.doe@example.com");

            assertEquals(email1, email2);
            assertEquals(email1.hashCode(), email2.hashCode());
        }

        @Test
        @DisplayName("Should be equal when emails are same but different case")
        void shouldBeEqualWhenEmailsAreSameButDifferentCase() {
            Email email1 = new Email("JOHN.DOE@EXAMPLE.COM");
            Email email2 = new Email("john.doe@example.com");

            assertEquals(email1, email2);
            assertEquals(email1.hashCode(), email2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal when emails are different")
        void shouldNotBeEqualWhenEmailsAreDifferent() {
            Email email1 = new Email("john.doe@example.com");
            Email email2 = new Email("jane.doe@example.com");

            assertNotEquals(email1, email2);
        }

        @Test
        @DisplayName("Should be equal to itself")
        void shouldBeEqualToItself() {
            Email email = new Email("john.doe@example.com");

            assertEquals(email, email);
        }

        @Test
        @DisplayName("Should not be equal to null")
        void shouldNotBeEqualToNull() {
            Email email = new Email("john.doe@example.com");

            assertNotEquals(email, null);
        }

        @Test
        @DisplayName("Should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            Email email = new Email("john.doe@example.com");
            String stringValue = "john.doe@example.com";

            assertNotEquals(email, stringValue);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should return normalized email value")
        void shouldReturnNormalizedEmailValue() {
            String emailValue = "  JOHN.DOE@EXAMPLE.COM  ";
            Email email = new Email(emailValue);

            assertEquals("john.doe@example.com", email.toString());
        }
    }
}
