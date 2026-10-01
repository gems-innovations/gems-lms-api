package com.gems.education.application.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CourseResponse {
  private final Long id;
  private final String title;
  private final String description;
  private final String status;
  private final String difficulty;
  private final List<String> tags;
  private final String thumbnailUrl;
  private final String instructorName;
  private final String institutionId;
  private final Integer totalDuration;
  private final Integer totalLessons;
  private final Integer enrolledCount;
  private final Integer completionRate;
  private final BigDecimal averageRating;
  private final Integer ratingCount;
  private final LocalDateTime publishedAt;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;
  private final List<ModuleResponse> modules;

  public CourseResponse(Long id, String title, String description, String status, String difficulty,
                        List<String> tags, String thumbnailUrl, String instructorName, String institutionId,
                        Integer totalDuration, Integer totalLessons, Integer enrolledCount, Integer completionRate,
                        BigDecimal averageRating, Integer ratingCount, LocalDateTime publishedAt,
                        LocalDateTime createdAt, LocalDateTime updatedAt, List<ModuleResponse> modules) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.status = status;
    this.difficulty = difficulty;
    this.tags = tags;
    this.thumbnailUrl = thumbnailUrl;
    this.instructorName = instructorName;
    this.institutionId = institutionId;
    this.totalDuration = totalDuration;
    this.totalLessons = totalLessons;
    this.enrolledCount = enrolledCount;
    this.completionRate = completionRate;
    this.averageRating = averageRating;
    this.ratingCount = ratingCount;
    this.publishedAt = publishedAt;
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

  public String getDifficulty() {
    return difficulty;
  }

  public List<String> getTags() {
    return tags;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public String getInstructorName() {
    return instructorName;
  }

  public String getInstitutionId() {
    return institutionId;
  }

  public Integer getTotalDuration() {
    return totalDuration;
  }

  public Integer getTotalLessons() {
    return totalLessons;
  }

  public Integer getEnrolledCount() {
    return enrolledCount;
  }

  public Integer getCompletionRate() {
    return completionRate;
  }

  public BigDecimal getAverageRating() {
    return averageRating;
  }

  public Integer getRatingCount() {
    return ratingCount;
  }

  public LocalDateTime getPublishedAt() {
    return publishedAt;
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
