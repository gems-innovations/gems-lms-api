package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
@DisplayName("Email Value Object Tests")
class EmailTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create Email with valid value")
    void shouldCreateValidEmail() {
      Email email = new Email("john.doe@example.com");

      assertNotNull(email);
      assertEquals("john.doe@example.com", email.getValue());
    }

    @Test
    @DisplayName("Should trim whitespace and convert to lowercase")
    void shouldTrimAndLowercase() {
      Email email = new Email("   JOHN.DOE@EXAMPLE.COM   ");

      assertEquals("john.doe@example.com", email.getValue());
    }

    @Test
    @DisplayName("Should throw exception when email is null")
    void shouldThrowWhenEmailIsNull() {
      IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new Email(null));

      assertEquals(StudentsConstants.EMAIL_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when email is empty")
    void shouldThrowWhenEmailIsEmpty() {
      IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new Email("   "));

      assertEquals(StudentsConstants.EMAIL_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when email format is invalid")
    void shouldThrowWhenFormatIsInvalid() {
      IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid-email"));

      assertEquals(StudentsConstants.INVALID_EMAIL_FORMAT, exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Format Validation Tests")
  class FormatValidationTests {

    @Test
    @DisplayName("Should accept valid email with plus tags")
    void shouldAcceptPlusTags() {
      Email email = new Email("john+tag@example.com");

      assertEquals("john+tag@example.com", email.getValue());
    }

    @Test
    @DisplayName("Should accept valid email with subdomains")
    void shouldAcceptSubdomains() {
      Email email = new Email("john.doe@mail.example.co.uk");

      assertEquals("john.doe@mail.example.co.uk", email.getValue());
    }

    @Test
    @DisplayName("Should reject email missing '@'")
    void shouldRejectMissingAtSymbol() {
      assertThrows(IllegalArgumentException.class, () -> new Email("john.example.com"));
    }

    @Test
    @DisplayName("Should reject email with invalid domain")
    void shouldRejectInvalidDomain() {
      assertThrows(IllegalArgumentException.class, () -> new Email("john@.com"));
    }
  }

  @Nested
  @DisplayName("Equality & HashCode Tests")
  class EqualityTests {

    @Test
    @DisplayName("Should consider two emails equal when normalized values match")
    void shouldBeEqualIfSameValue() {
      Email email1 = new Email("John.Doe@Example.com");
      Email email2 = new Email("  john.doe@example.com  ");

      assertEquals(email1, email2);
      assertEquals(email1.hashCode(), email2.hashCode());
    }

    @Test
    @DisplayName("Should consider two emails different when values differ")
    void shouldBeDifferentIfDifferentValue() {
      Email email1 = new Email("john1@example.com");
      Email email2 = new Email("john2@example.com");

      assertNotEquals(email1, email2);
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should return email value on toString")
    void shouldReturnValueInToString() {
      Email email = new Email("john.doe@example.com");

      assertEquals("john.doe@example.com", email.toString());
    }
  }
}