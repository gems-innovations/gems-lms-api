package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Table("courses")
public class CourseEntity {
  @Id
  private Long id;
  private String title;
  private String description;
  private String status;
  private String difficulty;

  /** Comma-separated tag list; kept as plain text to avoid Postgres array/R2DBC mapping friction. */
  private String tags;

  @Column("thumbnail_url")
  private String thumbnailUrl;

  @Column("instructor_name")
  private String instructorName;

  @Column("institution_id")
  private String institutionId;

  @Column("total_duration")
  private Integer totalDuration;

  @Column("total_lessons")
  private Integer totalLessons;

  @Column("enrolled_count")
  private Integer enrolledCount;

  @Column("completion_rate")
  private Integer completionRate;

  @Column("average_rating")
  private BigDecimal averageRating;

  @Column("rating_count")
  private Integer ratingCount;

  @Column("published_at")
  private LocalDateTime publishedAt;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  public CourseEntity() {
  }

  public CourseEntity(Long id, String title, String description, String status, String difficulty, String tags,
                      String thumbnailUrl, String instructorName, String institutionId, Integer totalDuration,
                      Integer totalLessons, Integer enrolledCount, Integer completionRate, BigDecimal averageRating,
                      Integer ratingCount, LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
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

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
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
}
