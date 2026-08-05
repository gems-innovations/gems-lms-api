package com.gems.education.application.response;

import java.time.LocalDateTime;
import java.util.List;

public class CourseResponse {
  private final Long id;
  private final String title;
  private final String description;
  private final String status;
  private final String institutionId;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;
  private final List<ModuleResponse> modules;

  public CourseResponse(Long id, String title, String description, String status, String institutionId,
                        LocalDateTime createdAt, LocalDateTime updatedAt, List<ModuleResponse> modules) {
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

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getStatus() {
    return status;
  }

  public String getInstitutionId() {
    return institutionId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public List<ModuleResponse> getModules() {
    return modules;
  }
}
