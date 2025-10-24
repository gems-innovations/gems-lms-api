package com.gems.auth.domain.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserNotFoundException Tests")
class UserNotFoundExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateExceptionWithMessage() {
            String message = "User with id 1 not found";
            UserNotFoundException exception = new UserNotFoundException(message);

            assertNotNull(exception);
            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            String message = "User with id 1 not found";
            RuntimeException cause = new RuntimeException("Database error");
            UserNotFoundException exception = new UserNotFoundException(message);
            exception.initCause(cause);

            assertNotNull(exception);
            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with null message")
        void shouldCreateExceptionWithNullMessage() {
            UserNotFoundException exception = new UserNotFoundException(null);

            assertNotNull(exception);
            assertNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should create exception with empty message")
        void shouldCreateExceptionWithEmptyMessage() {
            String message = "";
            UserNotFoundException exception = new UserNotFoundException(message);

            assertNotNull(exception);
            assertEquals(message, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTests {

        @Test
        @DisplayName("Should be instance of RuntimeException")
        void shouldBeInstanceOfRuntimeException() {
            UserNotFoundException exception = new UserNotFoundException("Test message");

            assertTrue(exception instanceof RuntimeException);
        }

        @Test
        @DisplayName("Should be instance of Exception")
        void shouldBeInstanceOfException() {
            UserNotFoundException exception = new UserNotFoundException("Test message");

            assertTrue(exception instanceof Exception);
        }

        @Test
        @DisplayName("Should be instance of Throwable")
        void shouldBeInstanceOfThrowable() {
            UserNotFoundException exception = new UserNotFoundException("Test message");

            assertTrue(exception instanceof Throwable);
        }
    }

    @Nested
    @DisplayName("Exception Behavior Tests")
    class ExceptionBehaviorTests {

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {
            assertThrows(UserNotFoundException.class, () -> {
                throw new UserNotFoundException("Test message");
            });
        }

        @Test
        @DisplayName("Should preserve stack trace")
        void shouldPreserveStackTrace() {
            UserNotFoundException exception = new UserNotFoundException("Test message");
            StackTraceElement[] stackTrace = exception.getStackTrace();

            assertNotNull(stackTrace);
            assertTrue(stackTrace.length > 0);
        }
    }
}
