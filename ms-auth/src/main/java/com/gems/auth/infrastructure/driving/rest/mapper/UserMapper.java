package com.gems.auth.infrastructure.driving.rest.mapper;

import com.gems.auth.application.command.RegisterUserCommand;
import com.gems.auth.infrastructure.driving.rest.request.RegisterUserRequest;

public class UserMapper {

  private UserMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static RegisterUserCommand toDomain(RegisterUserRequest request) {
    return new RegisterUserCommand(
      request.getName(),
      request.getEmail(),
      request.getPassword()
    );
  }
}
