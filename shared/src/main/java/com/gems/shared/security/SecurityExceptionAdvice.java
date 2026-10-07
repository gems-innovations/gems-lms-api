package com.gems.shared.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Answers authorization failures with 403 before each service's catch-all handler can
 * turn them into a 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionAdvice {

  @ExceptionHandler(ForbiddenException.class)
  public Mono<ResponseEntity<Map<String, Object>>> handleForbidden(ForbiddenException ex) {
    return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
      "code", "FORBIDDEN",
      "message", ex.getReason() != null ? ex.getReason() : "Forbidden",
      "status", HttpStatus.FORBIDDEN.value()
    )));
  }
}
