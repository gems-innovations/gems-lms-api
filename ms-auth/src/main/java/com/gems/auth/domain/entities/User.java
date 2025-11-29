package com.gems.auth.domain.entities;

import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.UserRole;

import java.time.LocalDateTime;

public class User {
  private UserId id;
  private final UserName name;
  private final Email email;
  private final Password password;
  private final UserRole role;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;
  private final Boolean active;

  public User(Long id, String name, String email, String password, UserRole role) {
    this.id = new UserId(id);
    this.name = new UserName(name);
    this.email = new Email(email);
    this.password = new Password(password);
    this.role = role;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.active = true;
  }

  public User(String name, String email, String password, UserRole role) {
    this.name = new UserName(name);
    this.email = new Email(email);
    this.password = new Password(password);
    this.role = role;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.active = true;
  }

  public User(UserId id, UserName name, Email email, Password password, UserRole role, LocalDateTime createdAt, LocalDateTime updatedAt, Boolean active) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = role;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.active = active;
  }

  public UserId getId() {
    return id;
  }

  public UserName getName() {
    return name;
  }

  public Email getEmail() {
    return email;
  }

  public Password getPassword() {
    return password;
  }

  public UserRole getRole() {
    return role;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public Boolean isActive() {
    return active;
  }
}
