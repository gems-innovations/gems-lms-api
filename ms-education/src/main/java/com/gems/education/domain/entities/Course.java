package com.gems.education.domain.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Course {
  private Long id;
  private String title;
  private String description;
  private String status;
  private String difficulty;
  private List<String> tags;
  private String thumbnailUrl;
  private String instructorName;
  private String institutionId;
  private Integer totalDuration;
  private Integer totalLessons;
  private Integer enrolledCount;
  private Integer completionRate;
  private BigDecimal averageRating;
  private Integer ratingCount;
  private LocalDateTime publishedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private List<Module> modules;

  public Course() {
  }

  public Course(Long id, String title, String description, String status, String difficulty, List<String> tags,
                String thumbnailUrl, String instructorName, String institutionId, Integer totalDuration,
                Integer totalLessons, Integer enrolledCount, Integer completionRate, BigDecimal averageRating,
                Integer ratingCount, LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
                List<Module> modules) {
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

  public String getDifficulty() {
    return difficulty;
  }

  public void setDifficulty(String difficulty) {
    this.difficulty = difficulty;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public String getInstructorName() {
    return instructorName;
  }

  public void setInstructorName(String instructorName) {
    this.instructorName = instructorName;
  }

  public String getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(String institutionId) {
    this.institutionId = institutionId;
  }

  public Integer getTotalDuration() {
    return totalDuration;
  }

  public void setTotalDuration(Integer totalDuration) {
    this.totalDuration = totalDuration;
  }

  public Integer getTotalLessons() {
    return totalLessons;
  }

  public void setTotalLessons(Integer totalLessons) {
    this.totalLessons = totalLessons;
  }

  public Integer getEnrolledCount() {
    return enrolledCount;
  }

  public void setEnrolledCount(Integer enrolledCount) {
    this.enrolledCount = enrolledCount;
  }

  public Integer getCompletionRate() {
    return completionRate;
  }

  public void setCompletionRate(Integer completionRate) {
    this.completionRate = completionRate;
  }

  public BigDecimal getAverageRating() {
    return averageRating;
  }

  public void setAverageRating(BigDecimal averageRating) {
    this.averageRating = averageRating;
  }

  public Integer getRatingCount() {
    return ratingCount;
  }

  public void setRatingCount(Integer ratingCount) {
    this.ratingCount = ratingCount;
  }

  public LocalDateTime getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(LocalDateTime publishedAt) {
    this.publishedAt = publishedAt;
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
