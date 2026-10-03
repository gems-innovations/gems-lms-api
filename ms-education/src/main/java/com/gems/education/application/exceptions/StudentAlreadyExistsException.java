package com.gems.education.application.exceptions;


public class StudentAlreadyExistsException extends RuntimeException {

  public StudentAlreadyExistsException(String message) {
    super(message);
  }

  public StudentAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }

}
