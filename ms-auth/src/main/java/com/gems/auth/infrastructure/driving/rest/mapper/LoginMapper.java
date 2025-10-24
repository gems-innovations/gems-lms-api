package com.gems.auth.infrastructure.driving.rest.mapper;

import com.gems.auth.application.command.LoginCommand;
import com.gems.auth.application.response.LoginResponse;
import com.gems.auth.infrastructure.driving.rest.request.LoginRequest;

public class LoginMapper {
  
  public static LoginCommand toCommand(LoginRequest request) {
    return new LoginCommand(request.getEmail(), request.getPassword());
  }
}
