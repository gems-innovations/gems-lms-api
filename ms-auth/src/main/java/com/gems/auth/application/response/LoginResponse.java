package com.gems.auth.application.response;

import com.gems.auth.domain.values.UserRole;
import java.util.Objects;

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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    LoginResponse that = (LoginResponse) obj;
    return Objects.equals(userId, that.userId) &&
           Objects.equals(name, that.name) &&
           Objects.equals(email, that.email) &&
           Objects.equals(role, that.role) &&
           Objects.equals(token, that.token);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, name, email, role, token);
  }
}
