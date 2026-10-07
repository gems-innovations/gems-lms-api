package com.gems.auth.application.response;

import java.time.LocalDateTime;

public record LoginResponse(
  Long userId,
  String firstName,
  String lastName,
  String username,
  String email,
  String role,
  String institutionId,
  String avatarUrl,
  boolean active,
  LocalDateTime createdAt,
  LocalDateTime updatedAt,
  String token,
  boolean mustChangePassword) {
}
