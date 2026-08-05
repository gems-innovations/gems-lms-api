package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class LearningPathRequest {
  @NotBlank(message = "Learning path title is required")
  private String title;

  private String description;

  @NotBlank(message = "Institution ID is required")
  private String institutionId;

  private List<Long> courseIds;

  public LearningPathRequest() {
  }

  public LearningPathRequest(String title, String description, String institutionId, List<Long> courseIds) {
    this.title = title;
    this.description = description;
    this.institutionId = institutionId;
    this.courseIds = courseIds;
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

  public List<Long> getCourseIds() {
    return courseIds;
  }

  public void setCourseIds(List<Long> courseIds) {
    this.courseIds = courseIds;
  }
}
