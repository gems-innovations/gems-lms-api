package com.gems.education.infrastructure.driving.rest.exeption;

public class QuizNotFoundException extends RuntimeException {
  public QuizNotFoundException(String message) {
    super(message);
  }
}
