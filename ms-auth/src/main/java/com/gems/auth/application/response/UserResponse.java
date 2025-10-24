package com.gems.auth.application.response;

import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.Email;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserResponse {
  private final Long id;
  private final String name;
  private final String email;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;
  private final boolean active;

  public UserResponse(Long id, UserName name, Email email, LocalDateTime createdAt, LocalDateTime updatedAt, boolean active) {
    this.id = id;
    this.name = name.getValue();
    this.email = email.getValue();
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.active = active;
  }

  public Long getId() {
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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    UserResponse that = (UserResponse) obj;
    return active == that.active &&
           Objects.equals(id, that.id) &&
           Objects.equals(name, that.name) &&
           Objects.equals(email, that.email) &&
           Objects.equals(createdAt, that.createdAt) &&
           Objects.equals(updatedAt, that.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email, createdAt, updatedAt, active);
  }
}
