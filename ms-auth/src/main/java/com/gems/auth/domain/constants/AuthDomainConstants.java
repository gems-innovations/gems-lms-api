package com.gems.auth.domain.constants;

public class AuthDomainConstants {
  public static final String USER_ID_CANNOT_BE_NULL_OR_EMPTY = "User ID cannot be null or empty";
  public static final String USER_NAME_CANNOT_BE_NULL_OR_EMPTY = "User name cannot be null or empty";
  public static final String USER_NAME_MIN_LENGTH = "User name must be at least 2 characters long";
  public static final String USER_NAME_MAX_LENGTH = "User name cannot exceed 50 characters";
  public static final String EMAIL_CANNOT_BE_NULL_OR_EMPTY = "Email cannot be null or empty";
  public static final String INVALID_EMAIL_FORMAT = "Invalid email format";
  public static final String PASSWORD_CANNOT_BE_NULL_OR_EMPTY = "Password cannot be null or empty";
  public static final String PASSWORD_MIN_LENGTH = "Password must be at least 8 characters long";
  public static final String PASSWORD_PATTERN = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character";
  public static final String INVALID_USER_ROLE = "Invalid UserRole";
  public static final int PASSWORD_MIN_LENGTH_VALUE = 8;
  public static final int USER_NAME_MIN_LENGTH_VALUE = 2;
  public static final int USER_NAME_MAX_LENGTH_VALUE = 50;
  public static final String EMAIL_PATTERN_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
  public static final String PASSWORD_PATTERN_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$";

  private AuthDomainConstants() {
    throw new UnsupportedOperationException("Utility class");
  }
}
