package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("lessons")
public class LessonEntity {
  @Id
  private Long id;

  @Column("module_id")
  private Long moduleId;

  private String title;

  @Column("order_index")
  private Integer orderIndex;

  @Column("created_at")
  private LocalDateTime createdAt;

  public LessonEntity() {
  }

  public LessonEntity(Long id, Long moduleId, String title, Integer orderIndex, LocalDateTime createdAt) {
    this.id = id;
    this.moduleId = moduleId;
    this.title = title;
    this.orderIndex = orderIndex;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getModuleId() {
    return moduleId;
  }

  public void setModuleId(Long moduleId) {
    this.moduleId = moduleId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public void setOrderIndex(Integer orderIndex) {
    this.orderIndex = orderIndex;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  /** Optional lesson description; isFree lessons can be previewed without enrolling. */
  @org.springframework.data.relational.core.mapping.Column("description")
  private String description;
  @org.springframework.data.relational.core.mapping.Column("is_free")
  private Boolean isFree = false;

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Boolean getIsFree() { return isFree; }
  public void setIsFree(Boolean isFree) { this.isFree = Boolean.TRUE.equals(isFree); }

  public LessonEntity details(String description, Boolean isFree) {
    this.description = description;
    this.isFree = Boolean.TRUE.equals(isFree);
    return this;
  }
}
