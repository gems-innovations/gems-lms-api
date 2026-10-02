package com.gems.education.domain.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LearningPath {
  private Long id;
  private String title;
  private String description;
  private String institutionId;
  private LocalDateTime createdAt;
  private List<Course> courses;
  private String status = DRAFT;
  private List<String> tags = new ArrayList<>();
  private String thumbnailUrl;
  private LocalDateTime updatedAt;
  /** Settings of each course in {@link #courses}; a course without an entry is a required step. */
  private List<LearningPathStep> steps = new ArrayList<>();
  private long enrolledCount;

  public static final String DRAFT = "draft";
  public static final String PUBLISHED = "published";
  public static final String ARCHIVED = "archived";

  public LearningPath() {
  }

  public LearningPath(Long id, String title, String description, String institutionId, LocalDateTime createdAt, List<Course> courses) {
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

  public List<Course> getCourses() {
    return courses;
  }

  public void setCourses(List<Course> courses) {
    this.courses = courses;
  }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public boolean isPublished() { return PUBLISHED.equals(status); }
  public List<String> getTags() { return tags; }
  public void setTags(List<String> tags) { this.tags = tags == null ? new ArrayList<>() : tags; }
  public String getThumbnailUrl() { return thumbnailUrl; }
  public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public List<LearningPathStep> getSteps() { return steps; }
  public void setSteps(List<LearningPathStep> steps) { this.steps = steps == null ? new ArrayList<>() : steps; }
  public long getEnrolledCount() { return enrolledCount; }
  public void setEnrolledCount(long enrolledCount) { this.enrolledCount = enrolledCount; }

  /** The settings of a course of this path (required, no minimum score if none were given). */
  public LearningPathStep stepFor(Long courseId) {
    return steps.stream().filter(s -> s.courseId().equals(courseId)).findFirst()
      .orElse(LearningPathStep.requiredCourse(courseId));
  }
}
