package com.gems.auth.infrastructure.driving.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "User information response")
public class UserResponseDto {
  @Schema(description = "Unique user identifier", example = "123e4567-e89b-12d3-a456-426614174000")
  private final String id;

  @Schema(description = "User's full name", example = "John Doe")
  private final String name;

  @Schema(description = "User's email address", example = "john.doe@example.com")
  private final String email;

  @Schema(description = "User creation timestamp", example = "2024-01-15T10:30:00")
  private final LocalDateTime createdAt;

  @Schema(description = "User last update timestamp", example = "2024-01-15T10:30:00")
  private final LocalDateTime updatedAt;

  @Schema(description = "User account status", example = "true")
  private final boolean active;

  public UserResponseDto(String id, String name, String email, LocalDateTime createdAt, LocalDateTime updatedAt, boolean active) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.active = active;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public boolean isActive() {
    return active;
  }
}
