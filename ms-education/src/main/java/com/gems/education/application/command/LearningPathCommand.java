package com.gems.education.application.command;

import com.gems.education.domain.entities.LearningPathStep;

import java.util.List;

public class LearningPathCommand {
  private String title;
  private String description;
  private String institutionId;
  private List<Long> courseIds;
  /** Optional fields: null keeps the current value on update. */
  private String status;
  private List<String> tags;
  private String thumbnailUrl;
  private List<LearningPathStep> steps;

  public LearningPathCommand() {
  }

  public LearningPathCommand(String title, String description, String institutionId, List<Long> courseIds) {
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
  public List<LearningPathStep> getSteps() { return steps; }
  public void setSteps(List<LearningPathStep> steps) { this.steps = steps; }
}
