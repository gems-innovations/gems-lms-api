package com.gems.auth.domain.exceptions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InvalidCredentialsException Tests")
class InvalidCredentialsExceptionTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create exception with message")
        void shouldCreateExceptionWithMessage() {
            String message = "Invalid credentials provided";
            InvalidCredentialsException exception = new InvalidCredentialsException(message);

            assertNotNull(exception);
            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            String message = "Invalid credentials provided";
            RuntimeException cause = new RuntimeException("Authentication error");
            InvalidCredentialsException exception = new InvalidCredentialsException(message);
            exception.initCause(cause);

            assertNotNull(exception);
            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }

        @Test
        @DisplayName("Should create exception with null message")
        void shouldCreateExceptionWithNullMessage() {
            InvalidCredentialsException exception = new InvalidCredentialsException(null);

            assertNotNull(exception);
            assertNull(exception.getMessage());
        }

        @Test
        @DisplayName("Should create exception with empty message")
        void shouldCreateExceptionWithEmptyMessage() {
            String message = "";
            InvalidCredentialsException exception = new InvalidCredentialsException(message);

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
            InvalidCredentialsException exception = new InvalidCredentialsException("Test message");

            assertTrue(exception instanceof RuntimeException);
        }

        @Test
        @DisplayName("Should be instance of Exception")
        void shouldBeInstanceOfException() {
            InvalidCredentialsException exception = new InvalidCredentialsException("Test message");

            assertTrue(exception instanceof Exception);
        }

        @Test
        @DisplayName("Should be instance of Throwable")
        void shouldBeInstanceOfThrowable() {
            InvalidCredentialsException exception = new InvalidCredentialsException("Test message");

            assertTrue(exception instanceof Throwable);
        }
    }

    @Nested
    @DisplayName("Exception Behavior Tests")
    class ExceptionBehaviorTests {

        @Test
        @DisplayName("Should be throwable")
        void shouldBeThrowable() {
            assertThrows(InvalidCredentialsException.class, () -> {
                throw new InvalidCredentialsException("Test message");
            });
        }

        @Test
        @DisplayName("Should preserve stack trace")
        void shouldPreserveStackTrace() {
            InvalidCredentialsException exception = new InvalidCredentialsException("Test message");
            StackTraceElement[] stackTrace = exception.getStackTrace();

            assertNotNull(stackTrace);
            assertTrue(stackTrace.length > 0);
        }
    }
}
