package com.gems.auth.application.response;

import com.gems.auth.domain.values.UserRole;

public class LoginResponse {
  private final Long userId;
  private final String name;
  private final String email;
  private final UserRole role;
  private final String token;

  public LoginResponse(Long userId, String name, String email, UserRole role, String token) {
    this.userId = userId;
    this.name = name;
    this.email = email;
    this.role = role;
    this.token = token;
  }

  public Long getUserId() {
    return userId;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public UserRole getRole() {
    return role;
  }

  public String getToken() {
    return token;
  }
}
