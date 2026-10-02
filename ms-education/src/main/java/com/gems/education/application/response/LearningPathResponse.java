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
  private String status = "published";
  private List<String> tags = List.of();
  private String thumbnailUrl;
  private LocalDateTime updatedAt;
  private List<StepResponse> steps = List.of();
  private long enrolledCount;

  /** Settings of one course of the path, in path order. */
  public record StepResponse(Long courseId, boolean required, Integer minimumScore) {
  }

  public LearningPathResponse() {
  }

  /** A copy of this path with other course bodies (e.g. without answer keys). */
  public LearningPathResponse withCourses(List<CourseResponse> otherCourses) {
    LearningPathResponse copy = new LearningPathResponse(id, title, description, institutionId, createdAt, otherCourses);
    copy.status = status;
    copy.tags = tags;
    copy.thumbnailUrl = thumbnailUrl;
    copy.updatedAt = updatedAt;
    copy.steps = steps;
    copy.enrolledCount = enrolledCount;
    return copy;
  }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public List<String> getTags() { return tags; }
  public void setTags(List<String> tags) { this.tags = tags; }
  public String getThumbnailUrl() { return thumbnailUrl; }
  public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public List<StepResponse> getSteps() { return steps; }
  public void setSteps(List<StepResponse> steps) { this.steps = steps; }
  public long getEnrolledCount() { return enrolledCount; }
  public void setEnrolledCount(long enrolledCount) { this.enrolledCount = enrolledCount; }

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
