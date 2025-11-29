package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Name Value Object Tests")
class NameTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create Name with valid value")
    void shouldCreateValidName() {
      Name name = new Name("John Doe");

      assertNotNull(name);
      assertEquals("John Doe", name.getValue());
    }

    @Test
    @DisplayName("Should trim leading/trailing whitespace")
    void shouldTrimWhitespace() {
      Name name = new Name("   John Doe   ");

      assertEquals("John Doe", name.getValue());
    }

    @Test
    @DisplayName("Should throw when name is null")
    void shouldThrowWhenNull() {
      IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> new Name(null)
      );

      assertEquals(StudentsConstants.STUDENT_NAME_CANNOT_BE_NULL_OR_EMPTY, ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when name is empty")
    void shouldThrowWhenEmpty() {
      IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> new Name("   ")
      );

      assertEquals(StudentsConstants.STUDENT_NAME_CANNOT_BE_NULL_OR_EMPTY, ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when name is shorter than minimum length")
    void shouldThrowWhenTooShort() {
      int min = StudentsConstants.NAME_MIN_LENGTH_VALUE;

      String tooShort = "a".repeat(min - 1);

      IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> new Name(tooShort)
      );

      assertEquals(StudentsConstants.NAME_MIN_LENGTH, ex.getMessage());
    }

    @Test
    @DisplayName("Should throw when name is longer than maximum length")
    void shouldThrowWhenTooLong() {
      int max = StudentsConstants.NAME_MAX_LENGTH_VALUE;

      String tooLong = "a".repeat(max + 1);

      IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> new Name(tooLong)
      );

      assertEquals(StudentsConstants.NAME_MAX_LENGTH, ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should allow names at exact minimum length")
    void shouldAllowMinLengthName() {
      int min = StudentsConstants.NAME_MIN_LENGTH_VALUE;

      String nameStr = "a".repeat(min);
      Name name = new Name(nameStr);

      assertEquals(nameStr, name.getValue());
    }

    @Test
    @DisplayName("Should allow names at exact maximum length")
    void shouldAllowMaxLengthName() {
      int max = StudentsConstants.NAME_MAX_LENGTH_VALUE;

      String nameStr = "a".repeat(max);
      Name name = new Name(nameStr);

      assertEquals(nameStr, name.getValue());
    }

    @Test
    @DisplayName("Should handle special characters in names")
    void shouldHandleSpecialCharacters() {
      String special = "Jöhn Dœ";

      Name name = new Name(special);

      assertEquals(special, name.getValue());
    }
  }

  @Nested
  @DisplayName("Equality & HashCode Tests")
  class EqualityTests {

    @Test
    @DisplayName("Should consider two names equal if values match")
    void shouldBeEqual() {
      Name name1 = new Name("John Doe");
      Name name2 = new Name("   John Doe   ");

      assertEquals(name1, name2);
      assertEquals(name1.hashCode(), name2.hashCode());
    }

    @Test
    @DisplayName("Should consider two names different if values differ")
    void shouldBeDifferent() {
      Name name1 = new Name("John Doe");
      Name name2 = new Name("Jane Doe");

      assertNotEquals(name1, name2);
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should return name value in toString")
    void shouldReturnValueInToString() {
      Name name = new Name("John Doe");

      assertEquals("John Doe", name.toString());
    }
  }
}