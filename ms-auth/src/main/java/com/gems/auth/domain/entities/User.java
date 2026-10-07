package com.gems.auth.domain.entities;

import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.UserRole;

import java.time.LocalDateTime;

public class User {
  private UserId id;
  private final UserName firstName;
  private final UserName lastName;
  private final String username;
  private final Email email;
  private final Password password;
  private final UserRole role;
  private String institutionId;
  private String avatarUrl;
  private final LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Boolean active;
  /** Set for accounts created with a temporary password until the user picks their own. */
  private boolean mustChangePassword;

  public User(Long id, String firstName, String lastName, String username, String email, String password,
              UserRole role, String institutionId, String avatarUrl) {
    this.id = new UserId(id);
    this.firstName = new UserName(firstName);
    this.lastName = new UserName(lastName);
    this.username = username;
    this.email = new Email(email);
    this.password = new Password(password);
    this.role = role;
    this.institutionId = institutionId;
    this.avatarUrl = avatarUrl;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.active = true;
  }

  public User(String firstName, String lastName, String username, String email, String password,
              UserRole role, String institutionId, String avatarUrl) {
    this.firstName = new UserName(firstName);
    this.lastName = new UserName(lastName);
    this.username = username;
    this.email = new Email(email);
    this.password = new Password(password);
    this.role = role;
    this.institutionId = institutionId;
    this.avatarUrl = avatarUrl;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.active = true;
  }

  public User(UserId id, UserName firstName, UserName lastName, String username, Email email, Password password,
              UserRole role, String institutionId, String avatarUrl, LocalDateTime createdAt,
              LocalDateTime updatedAt, Boolean active) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.username = username;
    this.email = email;
    this.password = password;
    this.role = role;
    this.institutionId = institutionId;
    this.avatarUrl = avatarUrl;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.active = active;
  }

  public UserId getId() {
    return id;
  }

  public UserName getFirstName() {
    return firstName;
  }

  public UserName getLastName() {
    return lastName;
  }

  public String getUsername() {
    return username;
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

  public String getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(String institutionId) {
    this.institutionId = institutionId;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
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

  public void deactivate() {
    this.active = false;
    this.updatedAt = LocalDateTime.now();
  }

  public void activate() {
    this.active = true;
    this.updatedAt = LocalDateTime.now();
  }

  public boolean mustChangePassword() {
    return mustChangePassword;
  }

  public void setMustChangePassword(boolean mustChangePassword) {
    this.mustChangePassword = mustChangePassword;
  }
}
