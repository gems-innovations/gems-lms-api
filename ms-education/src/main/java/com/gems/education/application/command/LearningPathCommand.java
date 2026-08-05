package com.gems.education.application.command;

import java.util.List;

public class LearningPathCommand {
  private String title;
  private String description;
  private String institutionId;
  private List<Long> courseIds;

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
}
