package com.gems.shared.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The caller is authenticated but not allowed to perform the operation (HTTP 403). */
public class ForbiddenException extends ResponseStatusException {

  public ForbiddenException(String reason) {
    super(HttpStatus.FORBIDDEN, reason);
  }
}
