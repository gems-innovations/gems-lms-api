package com.gems.auth.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("users")
public class UserEntity {
  @Id
  @Column("user_id")
  private Long userId;
  private String name;
  private String email;
  private String password;
  private String role;

  @Column("institution_id")
  private String institutionId;

  private Boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public UserEntity() {
  }

  public UserEntity(Long userId, String name, String email, String password, String role, String institutionId,
                    Boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.userId = userId;
    this.name = name;
    this.email = email;
    this.password = password;
    this.role = role;
    this.institutionId = institutionId;
    this.active = active;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(String institutionId) {
    this.institutionId = institutionId;
  }
}
