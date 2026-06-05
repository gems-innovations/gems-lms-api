package com.gems.education.infrastructure.driving.rest.exeption;

import com.gems.education.domain.constants.StudentsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StudentAlreadyExistsException Tests")
class StudentAlreadyExistsExceptionTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create exception with message only")
    void shouldCreateExceptionWithMessage() {
      String message = "Student already exists";

      StudentAlreadyExistsException exception =
        new StudentAlreadyExistsException(message);

      assertNotNull(exception);
      assertEquals(message, exception.getMessage());
      assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
      String message = "Student already exists";
      Throwable cause = new RuntimeException("Duplicate key");

      StudentAlreadyExistsException exception =
        new StudentAlreadyExistsException(message, cause);

      assertNotNull(exception);
      assertEquals(message, exception.getMessage());
      assertEquals(cause, exception.getCause());
    }
  }

  @Nested
  @DisplayName("Behavior Tests")
  class BehaviorTests {

    @Test
    @DisplayName("Should behave like a RuntimeException")
    void shouldBehaveLikeRuntimeException() {
      RuntimeException ex =
        new StudentAlreadyExistsException("Test error");

      assertThrows(RuntimeException.class, () -> { throw ex; });
    }

    @Test
    @DisplayName("Should allow StudentsConstants message")
    void shouldAllowConstantsMessage() {
      String email = "test@example.com";
      String message = String.format(
        StudentsConstants.STUDENT_ALREADY_EXISTS_EMAIL_MESSAGE,
        email
      );

      StudentAlreadyExistsException exception =
        new StudentAlreadyExistsException(message);

      assertEquals(message, exception.getMessage());
    }
  }
}