package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StudentId Value Object Tests")
class StudentIdTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create StudentId with valid value")
    void shouldCreateValidStudentId() {
      StudentId id = new StudentId(10L);

      assertNotNull(id);
      assertEquals(10L, id.getValue());
    }

    @Test
    @DisplayName("Should throw when id is null")
    void shouldThrowWhenIdIsNull() {
      IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> new StudentId(null)
      );

      assertEquals(StudentsConstants.STUDENT_ID_CANNOT_BE_NULL_OR_EMPTY, ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("Should return correct value")
    void shouldReturnValue() {
      StudentId id = new StudentId(5L);

      assertEquals(5L, id.getValue());
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should allow zero as a valid ID")
    void shouldAllowZero() {
      StudentId id = new StudentId(0L);

      assertEquals(0L, id.getValue());
    }

    @Test
    @DisplayName("Should allow negative values (domain permits it)")
    void shouldAllowNegativeValues() {
      StudentId id = new StudentId(-25L);

      assertEquals(-25L, id.getValue());
    }
  }

  @Nested
  @DisplayName("Equality & HashCode Tests")
  class EqualityTests {

    @Test
    @DisplayName("Should consider two StudentIds equal when values match")
    void shouldBeEqual() {
      StudentId id1 = new StudentId(10L);
      StudentId id2 = new StudentId(10L);

      assertEquals(id1, id2);
      assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("Should consider two StudentIds different when values differ")
    void shouldBeDifferent() {
      StudentId id1 = new StudentId(10L);
      StudentId id2 = new StudentId(11L);

      assertNotEquals(id1, id2);
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should return ID value as string")
    void shouldReturnStringValue() {
      StudentId id = new StudentId(100L);

      assertEquals("100", id.toString());
    }
  }

  @Nested
  @DisplayName("Generate Tests")
  class GenerateTests {

    @Test
    @DisplayName("generate() should return null (current implementation)")
    void generateShouldReturnNull() {
      assertNull(StudentId.generate());
    }
  }
}