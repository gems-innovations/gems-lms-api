package com.gems.education.infrastructure.driving.rest.exeption;

public class EnrollmentNotFoundException extends RuntimeException {
  public EnrollmentNotFoundException(String message) {
    super(message);
  }
}
