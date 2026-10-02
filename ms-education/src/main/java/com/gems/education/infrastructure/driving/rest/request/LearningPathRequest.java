package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public class LearningPathRequest {
  @NotBlank(message = "Learning path title is required")
  private String title;

  private String description;

  @NotBlank(message = "Institution ID is required")
  private String institutionId;

  private List<Long> courseIds;
  @Pattern(regexp = "draft|published|archived", message = "Status must be draft, published or archived")
  private String status;
  private List<String> tags;
  private String thumbnailUrl;
  /** Ordered courses with their settings; when present it replaces courseIds. */
  private List<StepRequest> steps;

  public record StepRequest(Long courseId, Boolean required, Integer minimumScore) {
  }

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

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public List<String> getTags() { return tags; }
  public void setTags(List<String> tags) { this.tags = tags; }
  public String getThumbnailUrl() { return thumbnailUrl; }
  public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
  public List<StepRequest> getSteps() { return steps; }
  public void setSteps(List<StepRequest> steps) { this.steps = steps; }
}
