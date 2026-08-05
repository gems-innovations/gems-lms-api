package com.gems.education.infrastructure.driving.rest.exeption;

public class CourseNotFoundException extends RuntimeException {
  public CourseNotFoundException(String message) {
    super(message);
  }
}
