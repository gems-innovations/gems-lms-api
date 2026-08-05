package com.gems.admin.application.exceptions;

public class InstitutionNotFoundException extends RuntimeException {
  public InstitutionNotFoundException(String message) {
    super(message);
  }
}
