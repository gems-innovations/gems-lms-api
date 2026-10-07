package com.gems.education.application.exceptions;

import java.util.List;

/** The enrollment rules of the course do not let this student in; reasons are EnrollmentPolicy codes. */
public class EnrollmentNotAllowedException extends RuntimeException {
  private final List<String> reasons;

  public EnrollmentNotAllowedException(List<String> reasons) {
    super("Enrollment not allowed: " + String.join(", ", reasons));
    this.reasons = List.copyOf(reasons);
  }

  public List<String> getReasons() {
    return reasons;
  }
}
