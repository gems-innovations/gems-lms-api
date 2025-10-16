package com.gems.auth.domain.exceptions;

import com.gems.auth.domain.constants.UserConstants;

public class UserAlreadyExistsException extends RuntimeException {
  public UserAlreadyExistsException(String message) {
    super(message);
  }

  public UserAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }

  public static UserAlreadyExistsException withEmail(String email) {
    return new UserAlreadyExistsException(
      String.format(UserConstants.USER_ALREADY_EXISTS_MESSAGE, email)
    );
  }
}
