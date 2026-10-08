package com.gems.admin.infrastructure.driving.rest;

import com.gems.admin.application.exceptions.BrandingNotFoundException;
import com.gems.admin.infrastructure.constants.AdminInfraConstants;
import com.gems.admin.infrastructure.driving.rest.response.ErrorResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BrandingNotFoundException.class)
  @ApiResponse(responseCode = "404", description = "Branding not found for the specified company")
  public Mono<ResponseEntity<ErrorResponse>> handleBrandingNotFoundException(BrandingNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
      AdminInfraConstants.BRANDING_NOT_FOUND_CODE,
      ex.getMessage(),
      HttpStatus.NOT_FOUND.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
  }

  @ExceptionHandler(com.gems.admin.application.exceptions.InstitutionNotFoundException.class)
  @ApiResponse(responseCode = "404", description = "Institution not found")
  public Mono<ResponseEntity<ErrorResponse>> handleInstitutionNotFoundException(com.gems.admin.application.exceptions.InstitutionNotFoundException ex) {
    ErrorResponse error = new ErrorResponse(
      AdminInfraConstants.INSTITUTION_NOT_FOUND_CODE,
      ex.getMessage(),
      HttpStatus.NOT_FOUND.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
  }

  @ExceptionHandler(com.gems.admin.application.exceptions.InstitutionAlreadyExistsException.class)
  @ApiResponse(responseCode = "400", description = "Institution already exists")
  public Mono<ResponseEntity<ErrorResponse>> handleInstitutionAlreadyExistsException(com.gems.admin.application.exceptions.InstitutionAlreadyExistsException ex) {
    ErrorResponse error = new ErrorResponse(
      AdminInfraConstants.INSTITUTION_ALREADY_EXISTS_CODE,
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
      .orElse(AdminInfraConstants.VALIDATION_FAILED);

    ErrorResponse error = new ErrorResponse(
      AdminInfraConstants.VALIDATION_ERROR_CODE,
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
      .orElse(AdminInfraConstants.VALIDATION_FAILED);

    ErrorResponse error = new ErrorResponse(
      AdminInfraConstants.VALIDATION_ERROR_CODE,
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
      .orElse(AdminInfraConstants.VALIDATION_FAILED);

    ErrorResponse error = new ErrorResponse(
      AdminInfraConstants.VALIDATION_ERROR_CODE,
      errorMessage,
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ApiResponse(responseCode = "400", description = "Validation error: illegal argument provided")
  public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(IllegalArgumentException ex) {
    ErrorResponse error = new ErrorResponse(
      AdminInfraConstants.VALIDATION_ERROR_CODE,
      ex.getMessage(),
      HttpStatus.BAD_REQUEST.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
  }

  // Framework errors (unknown route, wrong method, malformed body...) keep their own status
  // instead of being reported as a 500.
  @ExceptionHandler(ResponseStatusException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleResponseStatusException(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
    ErrorResponse error = new ErrorResponse(status.name(),
      ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(), status.value());
    return Mono.just(ResponseEntity.status(status).body(error));
  }

  @ExceptionHandler(Exception.class)
  @ApiResponse(responseCode = "500", description = "Internal server error: unexpected error occurred")
  public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
    // The cause goes to the log only: exception messages can expose SQL, paths or other internals.
    LOG.error("Unhandled exception", ex);
    ErrorResponse error = new ErrorResponse(
      AdminInfraConstants.INTERNAL_SERVER_ERROR_CODE,
      AdminInfraConstants.INTERNAL_SERVER_ERROR_MESSAGE,
      HttpStatus.INTERNAL_SERVER_ERROR.value()
    );
    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
  }
}
