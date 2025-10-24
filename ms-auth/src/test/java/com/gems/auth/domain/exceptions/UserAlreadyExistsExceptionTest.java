package com.gems.auth.domain.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserAlreadyExistsException Tests")
class UserAlreadyExistsExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateExceptionWithMessage() {
            String message = "User with email test@example.com already exists";
            UserAlreadyExistsException exception = new UserAlreadyExistsException(message);

            assertNotNull(exception);
            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            String message = "User with email test@example.com already exists";
            RuntimeException cause = new RuntimeException("Database error");
            UserAlreadyExistsException exception = new UserAlreadyExistsException(message, cause);

            assertNotNull(exception);
            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with null message")
        void shouldCreateExceptionWithNullMessage() {
            UserAlreadyExistsException exception = new UserAlreadyExistsException(null);

            assertNotNull(exception);
            assertNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should create exception with empty message")
        void shouldCreateExceptionWithEmptyMessage() {
            String message = "";
            UserAlreadyExistsException exception = new UserAlreadyExistsException(message);

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
            UserAlreadyExistsException exception = new UserAlreadyExistsException("Test message");

            assertTrue(exception instanceof RuntimeException);
        }

        @Test
        @DisplayName("Should be instance of Exception")
        void shouldBeInstanceOfException() {
            UserAlreadyExistsException exception = new UserAlreadyExistsException("Test message");

            assertTrue(exception instanceof Exception);
        }

        @Test
        @DisplayName("Should be instance of Throwable")
        void shouldBeInstanceOfThrowable() {
            UserAlreadyExistsException exception = new UserAlreadyExistsException("Test message");

            assertTrue(exception instanceof Throwable);
        }
    }

    @Nested
    @DisplayName("Exception Behavior Tests")
    class ExceptionBehaviorTests {

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {
            assertThrows(UserAlreadyExistsException.class, () -> {
                throw new UserAlreadyExistsException("Test message");
            });
        }

        @Test
        @DisplayName("Should preserve stack trace")
        void shouldPreserveStackTrace() {
            UserAlreadyExistsException exception = new UserAlreadyExistsException("Test message");
            StackTraceElement[] stackTrace = exception.getStackTrace();

            assertNotNull(stackTrace);
            assertTrue(stackTrace.length > 0);
        }
    }
}
