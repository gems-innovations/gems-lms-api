package com.gems.admin.domain.entities;

import java.time.LocalDateTime;

public class Institution {
  private String id;
  private String name;
  private String type;
  private String status;
  private Integer usersCount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private InstitutionMetadata metadata;

  public Institution() {
  }

  public Institution(String id, String name, String type, String status, Integer usersCount,
                     LocalDateTime createdAt, LocalDateTime updatedAt, InstitutionMetadata metadata) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.status = status;
    this.usersCount = usersCount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.metadata = metadata;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getUsersCount() {
    return usersCount;
  }

  public void setUsersCount(Integer usersCount) {
    this.usersCount = usersCount;
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

  public InstitutionMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(InstitutionMetadata metadata) {
    this.metadata = metadata;
  }
}
