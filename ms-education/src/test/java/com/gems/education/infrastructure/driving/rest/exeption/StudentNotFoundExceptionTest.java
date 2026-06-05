package com.gems.education.infrastructure.driving.rest.exeption;

import com.gems.education.domain.constants.StudentsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StudentNotFoundException Tests")
class StudentNotFoundExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateExceptionWithMessage() {
      String message = "Student not found";

      StudentNotFoundException exception =
        new StudentNotFoundException(message);

      assertNotNull(exception);
      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
    }
  }

  @Nested
  @DisplayName("Behavior Tests")
  class BehaviorTests {

    @Test
    @DisplayName("Should act as a RuntimeException")
    void shouldActAsRuntimeException() {
      RuntimeException exception =
        new StudentNotFoundException("Not found");

      assertThrows(RuntimeException.class, () -> {
        throw exception;
      });
    }

    @Test
    @DisplayName("Should allow message using StudentsConstants")
    void shouldAllowConstantsMessage() {
      Long id = 99L;
      String expectedMessage = String.format(
        StudentsConstants.STUDENT_NOT_FOUND_MESSAGE, id
      );

      StudentNotFoundException exception =
        new StudentNotFoundException(expectedMessage);

      assertEquals(expectedMessage, exception.getMessage());
    }
  }
}