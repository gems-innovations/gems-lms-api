package com.gems.auth.infrastructure.constants;

public class AuthInfraConstants {
  public static final String USER_ALREADY_EXISTS_CODE = "USER_ALREADY_EXISTS";
  public static final String USER_NOT_FOUND_CODE = "USER_NOT_FOUND";
  public static final String USER_DEACTIVATED_CODE = "USER_DEACTIVATED";
  public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
  public static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
  public static final String INTERNAL_SERVER_ERROR_CODE = "INTERNAL_SERVER_ERROR";
  public static final String AUTH_API_BASE_PATH = "/api/v1/auth";
  public static final String REGISTER_ENDPOINT = "/register";
  public static final String LOGIN_ENDPOINT = "/login";

  public static final String NAME_REQUIRED_MESSAGE = "Name is required";
  public static final String NAME_SIZE_MESSAGE = "Name must be between 2 and 50 characters";
  public static final String EMAIL_REQUIRED_MESSAGE = "Email is required";
  public static final String EMAIL_VALID_MESSAGE = "Email must be a valid email address";
  public static final String PASSWORD_REQUIRED_MESSAGE = "Password is required";
  public static final String PASSWORD_SIZE_MESSAGE = "Password must be at least 8 characters long";
  public static final String PASSWORD_PATTERN_MESSAGE = "Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character";
  public static final String PASSWORD_PATTERN_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
  public static final String VALIDATION_FAILED = "Validation failed";

  private AuthInfraConstants() {
    throw new UnsupportedOperationException("Utility class");
  }
}
