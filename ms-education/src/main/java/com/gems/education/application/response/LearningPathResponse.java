package com.gems.education.application.response;

import java.time.LocalDateTime;
import java.util.List;

public class LearningPathResponse {
  private Long id;
  private String title;
  private String description;
  private String institutionId;
  private LocalDateTime createdAt;
  private List<CourseResponse> courses;

  public LearningPathResponse() {
  }

  public LearningPathResponse(Long id, String title, String description, String institutionId, LocalDateTime createdAt, List<CourseResponse> courses) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.institutionId = institutionId;
    this.createdAt = createdAt;
    this.courses = courses;
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

  public List<CourseResponse> getCourses() {
    return courses;
  }

  public void setCourses(List<CourseResponse> courses) {
    this.courses = courses;
  }
}
