package com.gems.education.application.command;

import java.util.List;

public class CourseCommand {
  private final String title;
  private final String description;
  private final String status;
  private final String institutionId;
  private final List<ModuleCommand> modules;

  public CourseCommand(String title, String description, String status, String institutionId, List<ModuleCommand> modules) {
    this.title = title;
    this.description = description;
    this.status = status;
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

  public String getInstitutionId() {
    return institutionId;
  }

  public List<ModuleCommand> getModules() {
    return modules;
  }
}
