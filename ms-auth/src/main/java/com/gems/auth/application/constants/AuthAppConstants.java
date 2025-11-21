package com.gems.auth.application.constants;

public class AuthAppConstants {
  public static final String USER_ALREADY_EXISTS_MESSAGE = "User with email %s already exists";
  public static final String USER_NOT_FOUND_MESSAGE = "User with email %s not found";
  public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";
  public static final String USER_DEACTIVATED_MESSAGE = "User account is deactivated";

  private AuthAppConstants() {
    throw new UnsupportedOperationException("Utility class");
  }
}
