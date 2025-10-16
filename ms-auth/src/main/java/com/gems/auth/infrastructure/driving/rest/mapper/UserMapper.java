package com.gems.auth.infrastructure.driving.rest.mapper;

import com.gems.auth.application.response.UserResponse;
import com.gems.auth.infrastructure.driving.rest.dto.UserResponseDto;

public class UserMapper {

  private UserMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static UserResponseDto toDto(UserResponse userResponse) {
    return new UserResponseDto(
      userResponse.getId(),
      userResponse.getName(),
      userResponse.getEmail(),
      userResponse.getCreatedAt(),
      userResponse.getUpdatedAt(),
      userResponse.isActive()
    );
  }
}
