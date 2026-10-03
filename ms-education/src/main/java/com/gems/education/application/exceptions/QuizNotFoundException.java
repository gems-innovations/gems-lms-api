package com.gems.education.application.exceptions;

public class QuizNotFoundException extends RuntimeException {
  public QuizNotFoundException(String message) {
    super(message);
  }
}
