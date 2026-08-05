package com.gems.admin.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("institutions")
public class InstitutionEntity implements Persistable<String> {
  @Id
  private String id;
  private String name;
  private String type;
  private String status;

  @Column("users_count")
  private Integer usersCount;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @Transient
  private boolean isNew = true;

  public InstitutionEntity() {
  }

  public InstitutionEntity(String id, String name, String type, String status, Integer usersCount,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.status = status;
    this.usersCount = usersCount;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  @Override
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

  @Override
  public boolean isNew() {
    return isNew;
  }

  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }
}
