package com.gems.auth.application.response;

import java.time.LocalDateTime;

public record UserResponse(
  Long userId,
  String name,
  String email,
  String role,
  LocalDateTime createdAt,
  LocalDateTime updatedAt,
  boolean active) {
}