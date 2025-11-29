package com.gems.auth.infrastructure.driving.rest;

import com.gems.auth.application.exceptions.InvalidCredentialsException;
import com.gems.auth.application.exceptions.UserAlreadyExistsException;
import com.gems.auth.application.exceptions.UserDeactivatedException;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.infrastructure.constants.AuthInfraConstants;
import com.gems.auth.infrastructure.driving.rest.response.ErrorResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolationException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserAlreadyExistsException.class)
  @ApiResponse(responseCode = "409", description = "User already exists with the provided email")
  public Mono<ResponseEntity<ErrorResponse>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
    ErrorResponse error = new ErrorResponse(
      AuthInfraConstants.USER_ALREADY_EXISTS_CODE,
      ex.getMessage(),
      HttpStatus.CONFLICT.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
  }

  @ExceptionHandler(UserNotFoundException.class)
  @ApiResponse(responseCode = "404", description = "User not found with the provided email")
  public Mono<ResponseEntity<ErrorResponse>> handleUserNotFoundException(UserNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
      AuthInfraConstants.USER_NOT_FOUND_CODE,
      ex.getMessage(),
      HttpStatus.NOT_FOUND.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  @ApiResponse(responseCode = "401", description = "Invalid credentials or user account is deactivated")
  public Mono<ResponseEntity<ErrorResponse>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
    ErrorResponse error = new ErrorResponse(
      AuthInfraConstants.INVALID_CREDENTIALS,
      ex.getMessage(),
      HttpStatus.UNAUTHORIZED.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error));
  }

  @ExceptionHandler(UserDeactivatedException.class)
  @ApiResponse(responseCode = "404", description = "Invalid credentials or user account is deactivated")
  public Mono<ResponseEntity<ErrorResponse>> handleUserDeactivatedException(UserDeactivatedException ex) {
    ErrorResponse error = new ErrorResponse(
      AuthInfraConstants.USER_DEACTIVATED_CODE,
      ex.getMessage(),
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  @ApiResponse(responseCode = "400", description = "Validation error: invalid request data")
  public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex) {
    String errorMessage = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .reduce((msg1, msg2) -> msg1 + "; " + msg2)
        .orElse(AuthInfraConstants.VALIDATION_FAILED);
    
    ErrorResponse error = new ErrorResponse(
      AuthInfraConstants.VALIDATION_ERROR_CODE,
      errorMessage,
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ApiResponse(responseCode = "400", description = "Validation error: invalid method arguments")
  public Mono<ResponseEntity<ErrorResponse>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
    String errorMessage = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .reduce((msg1, msg2) -> msg1 + "; " + msg2)
        .orElse(AuthInfraConstants.VALIDATION_FAILED);
    
    ErrorResponse error = new ErrorResponse(
      AuthInfraConstants.VALIDATION_ERROR_CODE,
      errorMessage,
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ApiResponse(responseCode = "400", description = "Validation error: constraint violation")
  public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolationException(ConstraintViolationException ex) {
    String errorMessage = ex.getConstraintViolations()
        .stream()
        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
        .reduce((msg1, msg2) -> msg1 + "; " + msg2)
        .orElse(AuthInfraConstants.VALIDATION_FAILED);
    
    ErrorResponse error = new ErrorResponse(
      AuthInfraConstants.VALIDATION_ERROR_CODE,
      errorMessage,
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ApiResponse(responseCode = "400", description = "Validation error: illegal argument provided")
  public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException ex) {
    ErrorResponse error = new ErrorResponse(
      AuthInfraConstants.VALIDATION_ERROR_CODE,
      ex.getMessage(),
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  @ExceptionHandler(Exception.class)
  @ApiResponse(responseCode = "500", description = "Internal server error: unexpected error occurred")
  public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
    ErrorResponse error = new ErrorResponse(
      AuthInfraConstants.INTERNAL_SERVER_ERROR_CODE,
      ex.getMessage(),
      HttpStatus.INTERNAL_SERVER_ERROR.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
  }
}
