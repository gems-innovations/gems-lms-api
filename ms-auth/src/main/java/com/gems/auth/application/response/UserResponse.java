package com.gems.auth.application.response;

import java.time.LocalDateTime;

public record UserResponse(
  Long userId,
  String firstName,
  String lastName,
  String username,
  String email,
  String role,
  String institutionId,
  String avatarUrl,
  LocalDateTime createdAt,
  LocalDateTime updatedAt,
  boolean active,
  String temporaryPassword) {

  public UserResponse(Long userId, String firstName, String lastName, String username, String email, String role,
                       String institutionId, String avatarUrl, LocalDateTime createdAt, LocalDateTime updatedAt,
                       boolean active) {
    this(userId, firstName, lastName, username, email, role, institutionId, avatarUrl, createdAt, updatedAt, active, null);
  }
}
