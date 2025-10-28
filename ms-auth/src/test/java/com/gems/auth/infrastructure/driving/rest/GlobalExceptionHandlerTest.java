package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.domain.exceptions.UserAlreadyExistsException;
import com.gems.auth.domain.exceptions.UserNotFoundException;
import com.gems.auth.infrastructure.driving.rest.constants.RestConstants;
import com.gems.auth.infrastructure.driving.rest.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("UserAlreadyExistsException Tests")
    class UserAlreadyExistsExceptionTests {

        @Test
        @DisplayName("Should handle UserAlreadyExistsException correctly")
        void shouldHandleUserAlreadyExistsExceptionCorrectly() {
            String message = "User with email already exists";
            UserAlreadyExistsException exception = new UserAlreadyExistsException(message);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleUserAlreadyExistsException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.USER_ALREADY_EXISTS_CODE, response.getBody().getCode());
                    assertEquals(message, response.getBody().getMessage());
                    assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle UserAlreadyExistsException with null message")
        void shouldHandleUserAlreadyExistsExceptionWithNullMessage() {
            UserAlreadyExistsException exception = new UserAlreadyExistsException(null);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleUserAlreadyExistsException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.USER_ALREADY_EXISTS_CODE, response.getBody().getCode());
                    assertNull(response.getBody().getMessage());
                    assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle UserAlreadyExistsException with empty message")
        void shouldHandleUserAlreadyExistsExceptionWithEmptyMessage() {
            UserAlreadyExistsException exception = new UserAlreadyExistsException("");

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleUserAlreadyExistsException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.USER_ALREADY_EXISTS_CODE, response.getBody().getCode());
                    assertEquals("", response.getBody().getMessage());
                    assertEquals(HttpStatus.CONFLICT.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("UserNotFoundException Tests")
    class UserNotFoundExceptionTests {

        @Test
        @DisplayName("Should handle UserNotFoundException correctly")
        void shouldHandleUserNotFoundExceptionCorrectly() {
            String message = "User not found with ID: 123";
            UserNotFoundException exception = new UserNotFoundException(message);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleUserNotFoundException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.USER_NOT_FOUND_CODE, response.getBody().getCode());
                    assertEquals(message, response.getBody().getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle UserNotFoundException with null message")
        void shouldHandleUserNotFoundExceptionWithNullMessage() {
            UserNotFoundException exception = new UserNotFoundException(null);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleUserNotFoundException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.USER_NOT_FOUND_CODE, response.getBody().getCode());
                    assertNull(response.getBody().getMessage());
                    assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("WebExchangeBindException Tests")
    class WebExchangeBindExceptionTests {

        @Mock
        private BindingResult bindingResult;

        @Mock
        private FieldError fieldError1;

        @Mock
        private FieldError fieldError2;

        @Test
        @DisplayName("Should handle WebExchangeBindException with single field error")
        void shouldHandleWebExchangeBindExceptionWithSingleFieldError() {
            WebExchangeBindException exception = new WebExchangeBindException(null, bindingResult);
            
            when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1));
            when(fieldError1.getField()).thenReturn("email");
            when(fieldError1.getDefaultMessage()).thenReturn("Email is required");

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleValidationException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertEquals("email: Email is required", response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle WebExchangeBindException with multiple field errors")
        void shouldHandleWebExchangeBindExceptionWithMultipleFieldErrors() {
            WebExchangeBindException exception = new WebExchangeBindException(null, bindingResult);
            
            when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));
            when(fieldError1.getField()).thenReturn("email");
            when(fieldError1.getDefaultMessage()).thenReturn("Email is required");
            when(fieldError2.getField()).thenReturn("password");
            when(fieldError2.getDefaultMessage()).thenReturn("Password is too short");

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleValidationException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertEquals("email: Email is required; password: Password is too short", response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle WebExchangeBindException with no field errors")
        void shouldHandleWebExchangeBindExceptionWithNoFieldErrors() {
            WebExchangeBindException exception = new WebExchangeBindException(null, bindingResult);
            
            when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList());

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleValidationException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertEquals("Validation failed", response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException Tests")
    class MethodArgumentNotValidExceptionTests {

        @Mock
        private BindingResult bindingResult;

        @Mock
        private FieldError fieldError1;

        @Mock
        private FieldError fieldError2;

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException with single field error")
        void shouldHandleMethodArgumentNotValidExceptionWithSingleFieldError() {
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
            
            when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1));
            when(fieldError1.getField()).thenReturn("name");
            when(fieldError1.getDefaultMessage()).thenReturn("Name is required");

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertEquals("name: Name is required", response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException with multiple field errors")
        void shouldHandleMethodArgumentNotValidExceptionWithMultipleFieldErrors() {
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
            
            when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));
            when(fieldError1.getField()).thenReturn("name");
            when(fieldError1.getDefaultMessage()).thenReturn("Name is required");
            when(fieldError2.getField()).thenReturn("email");
            when(fieldError2.getDefaultMessage()).thenReturn("Email format is invalid");

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertEquals("name: Name is required; email: Email format is invalid", response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle MethodArgumentNotValidException with no field errors")
        void shouldHandleMethodArgumentNotValidExceptionWithNoFieldErrors() {
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);
            
            when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList());

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertEquals("Validation failed", response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("ConstraintViolationException Tests")
    class ConstraintViolationExceptionTests {

        @Mock
        private ConstraintViolation<?> violation1;

        @Mock
        private ConstraintViolation<?> violation2;

        @Test
        @DisplayName("Should handle ConstraintViolationException with single violation")
        void shouldHandleConstraintViolationExceptionWithSingleViolation() {
            Set<ConstraintViolation<?>> violations = new HashSet<>(Arrays.asList(violation1));
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
            
            when(violation1.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
            when(violation1.getPropertyPath().toString()).thenReturn("email");
            when(violation1.getMessage()).thenReturn("Email is required");

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleConstraintViolationException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertEquals("email: Email is required", response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle ConstraintViolationException with multiple violations")
        void shouldHandleConstraintViolationExceptionWithMultipleViolations() {
            Set<ConstraintViolation<?>> violations = new HashSet<>(Arrays.asList(violation1, violation2));
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
            
            when(violation1.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
            when(violation1.getPropertyPath().toString()).thenReturn("name");
            when(violation1.getMessage()).thenReturn("Name is required");
            
            when(violation2.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
            when(violation2.getPropertyPath().toString()).thenReturn("password");
            when(violation2.getMessage()).thenReturn("Password is too short");

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleConstraintViolationException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    String message = response.getBody().getMessage();
                    assertTrue(message.contains("name: Name is required"));
                    assertTrue(message.contains("password: Password is too short"));
                    assertTrue(message.contains("; "));
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle ConstraintViolationException with no violations")
        void shouldHandleConstraintViolationExceptionWithNoViolations() {
            Set<ConstraintViolation<?>> violations = new HashSet<>();
            ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleConstraintViolationException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertEquals("Validation failed", response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("IllegalArgumentException Tests")
    class IllegalArgumentExceptionTests {

        @Test
        @DisplayName("Should handle IllegalArgumentException correctly")
        void shouldHandleIllegalArgumentExceptionCorrectly() {
            String message = "Invalid argument provided";
            IllegalArgumentException exception = new IllegalArgumentException(message);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleIllegalArgumentException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertEquals(message, response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException with null message")
        void shouldHandleIllegalArgumentExceptionWithNullMessage() {
            IllegalArgumentException exception = new IllegalArgumentException((String) null);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleIllegalArgumentException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertNull(response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle IllegalArgumentException with empty message")
        void shouldHandleIllegalArgumentExceptionWithEmptyMessage() {
            IllegalArgumentException exception = new IllegalArgumentException("");

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleIllegalArgumentException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, response.getBody().getCode());
                    assertEquals("", response.getBody().getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Generic Exception Tests")
    class GenericExceptionTests {

        @Test
        @DisplayName("Should handle generic Exception correctly")
        void shouldHandleGenericExceptionCorrectly() {
            String message = "An unexpected error occurred";
            Exception exception = new Exception(message);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleGenericException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.INTERNAL_SERVER_ERROR_CODE, response.getBody().getCode());
                    assertEquals(message, response.getBody().getMessage());
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle generic Exception with null message")
        void shouldHandleGenericExceptionWithNullMessage() {
            Exception exception = new Exception((String) null);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleGenericException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.INTERNAL_SERVER_ERROR_CODE, response.getBody().getCode());
                    assertNull(response.getBody().getMessage());
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle RuntimeException")
        void shouldHandleRuntimeException() {
            String message = "Runtime error occurred";
            RuntimeException exception = new RuntimeException(message);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleGenericException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.INTERNAL_SERVER_ERROR_CODE, response.getBody().getCode());
                    assertEquals(message, response.getBody().getMessage());
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should handle NullPointerException")
        void shouldHandleNullPointerException() {
            NullPointerException exception = new NullPointerException("Null pointer");

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleGenericException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(RestConstants.INTERNAL_SERVER_ERROR_CODE, response.getBody().getCode());
                    assertEquals("Null pointer", response.getBody().getMessage());
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
                    return true;
                })
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Error Response Structure Tests")
    class ErrorResponseStructureTests {

        @Test
        @DisplayName("Should create ErrorResponse with correct structure for UserAlreadyExistsException")
        void shouldCreateErrorResponseWithCorrectStructureForUserAlreadyExistsException() {
            String message = "User already exists";
            UserAlreadyExistsException exception = new UserAlreadyExistsException(message);

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleUserAlreadyExistsException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    ErrorResponse errorResponse = response.getBody();
                    assertNotNull(errorResponse);
                    
                    assertEquals(RestConstants.USER_ALREADY_EXISTS_CODE, errorResponse.getCode());
                    assertEquals(message, errorResponse.getMessage());
                    assertEquals(HttpStatus.CONFLICT.value(), errorResponse.getStatus());
                    
                    return true;
                })
                .verifyComplete();
        }

        @Test
        @DisplayName("Should create ErrorResponse with correct structure for validation errors")
        void shouldCreateErrorResponseWithCorrectStructureForValidationErrors() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid input");

            Mono<ResponseEntity<ErrorResponse>> result = globalExceptionHandler.handleIllegalArgumentException(exception);

            StepVerifier.create(result)
                .expectNextMatches(response -> {
                    ErrorResponse errorResponse = response.getBody();
                    assertNotNull(errorResponse);
                    
                    assertEquals(RestConstants.VALIDATION_ERROR_CODE, errorResponse.getCode());
                    assertEquals("Invalid input", errorResponse.getMessage());
                    assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus());
                    
                    return true;
                })
                .verifyComplete();
        }
    }
}
