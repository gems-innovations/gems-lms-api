package com.gems.auth.application.exceptions;

/** A password change or reset that was rejected; {@code code} tells the client why. */
public class PasswordChangeException extends RuntimeException {
  public static final String WRONG_CURRENT_PASSWORD = "WRONG_CURRENT_PASSWORD";
  public static final String SAME_PASSWORD = "SAME_PASSWORD";
  public static final String INVALID_RESET_TOKEN = "INVALID_RESET_TOKEN";

  private final String code;

  public PasswordChangeException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
