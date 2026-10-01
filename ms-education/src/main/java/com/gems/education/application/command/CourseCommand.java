package com.gems.education.application.command;

import java.util.List;

public class CourseCommand {
  private final String title;
  private final String description;
  private final String status;
  private final String difficulty;
  private final List<String> tags;
  private final String thumbnailUrl;
  private final String instructorName;
  private final String institutionId;
  private final List<ModuleCommand> modules;

  public CourseCommand(String title, String description, String status, String difficulty, List<String> tags,
                       String thumbnailUrl, String instructorName, String institutionId, List<ModuleCommand> modules) {
    this.title = title;
    this.description = description;
    this.status = status;
    this.difficulty = difficulty;
    this.tags = tags;
    this.thumbnailUrl = thumbnailUrl;
    this.instructorName = instructorName;
    this.institutionId = institutionId;
    this.modules = modules;
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

  public List<ModuleCommand> getModules() {
    return modules;
  }
}
