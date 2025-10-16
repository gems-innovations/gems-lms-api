package com.gems.auth.domain.entities;

import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserName;

import java.time.LocalDateTime;

public class User {
  private UserId id;
  private UserName name;
  private Email email;
  private Password password;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Boolean active;

  public User(UserId id, UserName name, Email email, Password password) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.password = password;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.active = true;
  }

  public User(String id, String name, String email, String password) {
    this.id = new UserId(id);
    this.name = new UserName(name);
    this.email = new Email(email);
    this.password = new Password(password);
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.active = true;
  }

  // Getters
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public Boolean isActive() {
    return active;
  }

  // Business methods
  public void updatePassword(Password newPassword) {
    this.password = newPassword;
    this.updatedAt = LocalDateTime.now();
  }

  public void deactivate() {
    this.active = false;
    this.updatedAt = LocalDateTime.now();
  }

  public void activate() {
    this.active = true;
    this.updatedAt = LocalDateTime.now();
  }
}
