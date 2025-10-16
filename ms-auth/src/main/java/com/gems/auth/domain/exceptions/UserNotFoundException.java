package com.gems.auth.domain.exceptions;

import com.gems.auth.domain.constants.UserConstants;

public class UserNotFoundException extends RuntimeException {
  public UserNotFoundException(String message) {
    super(message);
  }

  public UserNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public static UserNotFoundException withId(String id) {
    return new UserNotFoundException(
      String.format(UserConstants.USER_NOT_FOUND_MESSAGE, id)
    );
  }

  public static UserNotFoundException withEmail(String email) {
    return new UserNotFoundException(
      String.format(UserConstants.USER_NOT_FOUND_BY_EMAIL_MESSAGE, email)
    );
  }
}
