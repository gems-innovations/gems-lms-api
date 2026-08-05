package com.gems.admin.application.exceptions;

public class InstitutionAlreadyExistsException extends RuntimeException {
  public InstitutionAlreadyExistsException(String message) {
    super(message);
  }
}
