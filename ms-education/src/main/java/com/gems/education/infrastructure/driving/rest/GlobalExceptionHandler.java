package com.gems.education.infrastructure.driving.rest;


import com.gems.education.infrastructure.driving.rest.constants.RestConstants;
import com.gems.education.infrastructure.driving.rest.exeption.StudentAlreadyExistsException;
import com.gems.education.infrastructure.driving.rest.exeption.StudentNotFoundException;
import com.gems.education.infrastructure.driving.rest.response.ErrorResponse;
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

  @ExceptionHandler(StudentAlreadyExistsException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleStudentAlreadyExistsException(StudentAlreadyExistsException ex) {
    ErrorResponse error = new ErrorResponse(
      RestConstants.STUDENT_ALREADY_EXISTS_CODE,
      ex.getMessage(),
      HttpStatus.CONFLICT.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(error));
  }

  @ExceptionHandler(StudentNotFoundException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleStudentNotFoundException(StudentNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
      RestConstants.STUDENT_NOT_FOUND_CODE,
      ex.getMessage(),
      HttpStatus.NOT_FOUND.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
  }

  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex) {
    String errorMessage = ex.getBindingResult()
      .getFieldErrors()
      .stream()
      .map(error -> error.getField() + ": " + error.getDefaultMessage())
      .reduce((msg1, msg2) -> msg1 + "; " + msg2)
      .orElse("Validation failed");

    ErrorResponse error = new ErrorResponse(
      RestConstants.VALIDATION_ERROR_CODE,
      errorMessage,
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
    String errorMessage = ex.getBindingResult()
      .getFieldErrors()
      .stream()
      .map(error -> error.getField() + ": " + error.getDefaultMessage())
      .reduce((msg1, msg2) -> msg1 + "; " + msg2)
      .orElse("Validation failed");

    ErrorResponse error = new ErrorResponse(
      RestConstants.VALIDATION_ERROR_CODE,
      errorMessage,
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolationException(ConstraintViolationException ex) {
    String errorMessage = ex.getConstraintViolations()
      .stream()
      .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
      .reduce((msg1, msg2) -> msg1 + "; " + msg2)
      .orElse("Validation failed");

    ErrorResponse error = new ErrorResponse(
      RestConstants.VALIDATION_ERROR_CODE,
      errorMessage,
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException ex) {
    ErrorResponse error = new ErrorResponse(
      RestConstants.VALIDATION_ERROR_CODE,
      ex.getMessage(),
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
    ErrorResponse error = new ErrorResponse(
      RestConstants.INTERNAL_SERVER_ERROR_CODE,
      RestConstants.INTERNAL_SERVER_ERROR_MESSAGE,
      HttpStatus.INTERNAL_SERVER_ERROR.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
  }
}

