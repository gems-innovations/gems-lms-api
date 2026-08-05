package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("learning_paths")
public class LearningPathEntity {
  @Id
  private Long id;
  private String title;
  private String description;

  @Column("institution_id")
  private String institutionId;

  @Column("created_at")
  private LocalDateTime createdAt;

  public LearningPathEntity() {
  }

  public LearningPathEntity(Long id, String title, String description, String institutionId, LocalDateTime createdAt) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.institutionId = institutionId;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(String institutionId) {
    this.institutionId = institutionId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
