package com.gems.auth.infrastructure.driving.rest.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorResponse Tests")
class ErrorResponseTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ErrorResponse with all parameters")
        void shouldCreateErrorResponseWithAllParameters() {
            String code = "USER_NOT_FOUND";
            String message = "User not found";
            int status = 404;

            ErrorResponse errorResponse = new ErrorResponse(code, message, status);

            assertNotNull(errorResponse);
            assertEquals(code, errorResponse.getCode());
            assertEquals(message, errorResponse.getMessage());
            assertEquals(status, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should create ErrorResponse with null code")
        void shouldCreateErrorResponseWithNullCode() {
            String message = "Error message";
            int status = 400;

            ErrorResponse errorResponse = new ErrorResponse(null, message, status);

            assertNotNull(errorResponse);
            assertNull(errorResponse.getCode());
            assertEquals(message, errorResponse.getMessage());
            assertEquals(status, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should create ErrorResponse with null message")
        void shouldCreateErrorResponseWithNullMessage() {
            String code = "VALIDATION_ERROR";
            int status = 400;

            ErrorResponse errorResponse = new ErrorResponse(code, null, status);

            assertNotNull(errorResponse);
            assertEquals(code, errorResponse.getCode());
            assertNull(errorResponse.getMessage());
            assertEquals(status, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should create ErrorResponse with empty strings")
        void shouldCreateErrorResponseWithEmptyStrings() {
            String code = "";
            String message = "";
            int status = 500;

            ErrorResponse errorResponse = new ErrorResponse(code, message, status);

            assertNotNull(errorResponse);
            assertEquals(code, errorResponse.getCode());
            assertEquals(message, errorResponse.getMessage());
            assertEquals(status, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should create ErrorResponse with zero status")
        void shouldCreateErrorResponseWithZeroStatus() {
            String code = "TEST_ERROR";
            String message = "Test message";

            ErrorResponse errorResponse = new ErrorResponse(code, message, 0);

            assertNotNull(errorResponse);
            assertEquals(code, errorResponse.getCode());
            assertEquals(message, errorResponse.getMessage());
            assertEquals(0, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should create ErrorResponse with negative status")
        void shouldCreateErrorResponseWithNegativeStatus() {
            String code = "TEST_ERROR";
            String message = "Test message";

            ErrorResponse errorResponse = new ErrorResponse(code, message, -1);

            assertNotNull(errorResponse);
            assertEquals(code, errorResponse.getCode());
            assertEquals(message, errorResponse.getMessage());
            assertEquals(-1, errorResponse.getStatus());
        }
    }

    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct code")
        void shouldReturnCorrectCode() {
            String expectedCode = "USER_ALREADY_EXISTS";
            ErrorResponse errorResponse = new ErrorResponse(expectedCode, "Message", 409);

            String actualCode = errorResponse.getCode();

            assertEquals(expectedCode, actualCode);
        }

        @Test
        @DisplayName("Should return correct message")
        void shouldReturnCorrectMessage() {
            String expectedMessage = "User already exists with this email";
            ErrorResponse errorResponse = new ErrorResponse("CODE", expectedMessage, 409);

            String actualMessage = errorResponse.getMessage();

            assertEquals(expectedMessage, actualMessage);
        }

        @Test
        @DisplayName("Should return correct status")
        void shouldReturnCorrectStatus() {
            int expectedStatus = 500;
            ErrorResponse errorResponse = new ErrorResponse("CODE", "Message", expectedStatus);

            int actualStatus = errorResponse.getStatus();

            assertEquals(expectedStatus, actualStatus);
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("Should be immutable - code cannot be changed")
        void shouldBeImmutableCodeCannotBeChanged() {
            String originalCode = "ORIGINAL_CODE";
            ErrorResponse errorResponse = new ErrorResponse(originalCode, "Message", 400);

            String retrievedCode = errorResponse.getCode();

            assertEquals(originalCode, retrievedCode);
            assertSame(originalCode, retrievedCode);
        }

        @Test
        @DisplayName("Should be immutable - message cannot be changed")
        void shouldBeImmutableMessageCannotBeChanged() {
            String originalMessage = "Original message";
            ErrorResponse errorResponse = new ErrorResponse("CODE", originalMessage, 400);

            String retrievedMessage = errorResponse.getMessage();

            assertEquals(originalMessage, retrievedMessage);
            assertSame(originalMessage, retrievedMessage);
        }

        @Test
        @DisplayName("Should be immutable - status cannot be changed")
        void shouldBeImmutableStatusCannotBeChanged() {
            int originalStatus = 404;
            ErrorResponse errorResponse = new ErrorResponse("CODE", "Message", originalStatus);

            int retrievedStatus = errorResponse.getStatus();

            assertEquals(originalStatus, retrievedStatus);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle very long code")
        void shouldHandleVeryLongCode() {
            String longCode = "A".repeat(1000);
            ErrorResponse errorResponse = new ErrorResponse(longCode, "Message", 400);

            assertEquals(longCode, errorResponse.getCode());
        }

        @Test
        @DisplayName("Should handle very long message")
        void shouldHandleVeryLongMessage() {
            String longMessage = "This is a very long error message that contains a lot of text and should be handled properly by the ErrorResponse class. ".repeat(10);
            ErrorResponse errorResponse = new ErrorResponse("CODE", longMessage, 400);

            assertEquals(longMessage, errorResponse.getMessage());
        }

        @Test
        @DisplayName("Should handle special characters in code")
        void shouldHandleSpecialCharactersInCode() {
            String specialCode = "ERROR_CODE_@#$%^&*()";
            ErrorResponse errorResponse = new ErrorResponse(specialCode, "Message", 400);

            assertEquals(specialCode, errorResponse.getCode());
        }

        @Test
        @DisplayName("Should handle special characters in message")
        void shouldHandleSpecialCharactersInMessage() {
            String specialMessage = "Error message with special chars: @#$%^&*() and unicode: ñáéíóú";
            ErrorResponse errorResponse = new ErrorResponse("CODE", specialMessage, 400);

            assertEquals(specialMessage, errorResponse.getMessage());
        }

        @Test
        @DisplayName("Should handle whitespace in code")
        void shouldHandleWhitespaceInCode() {
            String codeWithWhitespace = "  ERROR_CODE  ";
            ErrorResponse errorResponse = new ErrorResponse(codeWithWhitespace, "Message", 400);

            assertEquals(codeWithWhitespace, errorResponse.getCode());
        }

        @Test
        @DisplayName("Should handle whitespace in message")
        void shouldHandleWhitespaceInMessage() {
            String messageWithWhitespace = "  Error message with whitespace  ";
            ErrorResponse errorResponse = new ErrorResponse("CODE", messageWithWhitespace, 400);

            assertEquals(messageWithWhitespace, errorResponse.getMessage());
        }
    }

    @Nested
    @DisplayName("Common HTTP Status Codes Tests")
    class CommonHttpStatusCodesTests {

        @Test
        @DisplayName("Should handle 400 Bad Request")
        void shouldHandle400BadRequest() {
            ErrorResponse errorResponse = new ErrorResponse("BAD_REQUEST", "Invalid request", 400);

            assertEquals(400, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should handle 401 Unauthorized")
        void shouldHandle401Unauthorized() {
            ErrorResponse errorResponse = new ErrorResponse("UNAUTHORIZED", "Authentication required", 401);

            assertEquals(401, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should handle 403 Forbidden")
        void shouldHandle403Forbidden() {
            ErrorResponse errorResponse = new ErrorResponse("FORBIDDEN", "Access denied", 403);

            assertEquals(403, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should handle 404 Not Found")
        void shouldHandle404NotFound() {
            ErrorResponse errorResponse = new ErrorResponse("NOT_FOUND", "Resource not found", 404);

            assertEquals(404, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should handle 409 Conflict")
        void shouldHandle409Conflict() {
            ErrorResponse errorResponse = new ErrorResponse("CONFLICT", "Resource already exists", 409);

            assertEquals(409, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should handle 500 Internal Server Error")
        void shouldHandle500InternalServerError() {
            ErrorResponse errorResponse = new ErrorResponse("INTERNAL_ERROR", "Internal server error", 500);

            assertEquals(500, errorResponse.getStatus());
        }
    }

    @Nested
    @DisplayName("Real World Scenarios Tests")
    class RealWorldScenariosTests {

        @Test
        @DisplayName("Should handle user registration validation error")
        void shouldHandleUserRegistrationValidationError() {
            ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "email: Email is required; password: Password must be at least 8 characters",
                400
            );

            assertEquals("VALIDATION_ERROR", errorResponse.getCode());
            assertEquals("email: Email is required; password: Password must be at least 8 characters", errorResponse.getMessage());
            assertEquals(400, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should handle user not found error")
        void shouldHandleUserNotFoundError() {
            ErrorResponse errorResponse = new ErrorResponse(
                "USER_NOT_FOUND",
                "User with ID 123 not found",
                404
            );

            assertEquals("USER_NOT_FOUND", errorResponse.getCode());
            assertEquals("User with ID 123 not found", errorResponse.getMessage());
            assertEquals(404, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should handle user already exists error")
        void shouldHandleUserAlreadyExistsError() {
            ErrorResponse errorResponse = new ErrorResponse(
                "USER_ALREADY_EXISTS",
                "User with email john@example.com already exists",
                409
            );

            assertEquals("USER_ALREADY_EXISTS", errorResponse.getCode());
            assertEquals("User with email john@example.com already exists", errorResponse.getMessage());
            assertEquals(409, errorResponse.getStatus());
        }

        @Test
        @DisplayName("Should handle internal server error")
        void shouldHandleInternalServerError() {
            ErrorResponse errorResponse = new ErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred while processing the request",
                500
            );

            assertEquals("INTERNAL_SERVER_ERROR", errorResponse.getCode());
            assertEquals("An unexpected error occurred while processing the request", errorResponse.getMessage());
            assertEquals(500, errorResponse.getStatus());
        }
    }
}
