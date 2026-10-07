package com.gems.education.application.exceptions;

public class EnrollmentNotFoundException extends RuntimeException {
  public EnrollmentNotFoundException(String message) {
    super(message);
  }
}
