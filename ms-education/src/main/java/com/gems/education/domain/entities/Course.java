package com.gems.education.domain.entities;

import java.time.LocalDateTime;
import java.util.List;

public class Course {
  private Long id;
  private String title;
  private String description;
  private String status;
  private String institutionId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<Module> modules;

  public Course() {
  }

  public Course(Long id, String title, String description, String status, String institutionId,
                LocalDateTime createdAt, LocalDateTime updatedAt, List<Module> modules) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.status = status;
    this.institutionId = institutionId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.modules = modules;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public List<Module> getModules() {
    return modules;
  }

  public void setModules(List<Module> modules) {
    this.modules = modules;
  }
}
