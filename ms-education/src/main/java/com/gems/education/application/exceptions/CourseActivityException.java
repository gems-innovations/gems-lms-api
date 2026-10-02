package com.gems.education.application.exceptions;

/** Rejections of quiz attempts and assignment submissions; {@code code} tells the client why. */
public class CourseActivityException extends RuntimeException {
  public static final String NOT_ENROLLED = "NOT_ENROLLED";
  public static final String BLOCK_NOT_FOUND = "BLOCK_NOT_FOUND";
  public static final String WRONG_BLOCK_TYPE = "WRONG_BLOCK_TYPE";
  public static final String ATTEMPT_LIMIT_REACHED = "ATTEMPT_LIMIT_REACHED";
  public static final String SUBMISSION_NOT_FOUND = "SUBMISSION_NOT_FOUND";

  private final String code;

  public CourseActivityException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
