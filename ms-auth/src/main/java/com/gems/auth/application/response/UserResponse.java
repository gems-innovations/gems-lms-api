package com.gems.auth.application.response;

import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.Email;

import java.time.LocalDateTime;

public class UserResponse {
  private final String id;
  private final String name;
  private final String email;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;
  private final boolean active;

  public UserResponse(UserId id, UserName name, Email email, LocalDateTime createdAt, LocalDateTime updatedAt, boolean active) {
    this.id = id.getValue();
    this.name = name.getValue();
    this.email = email.getValue();
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
