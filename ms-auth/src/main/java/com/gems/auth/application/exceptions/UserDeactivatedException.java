package com.gems.auth.application.exceptions;

public class UserDeactivatedException extends RuntimeException {
  public UserDeactivatedException(String message) {
    super(message);
  }
}
