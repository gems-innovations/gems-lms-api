package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CourseRequest {
  @NotBlank(message = "Course title is required")
  private String title;

  private String description;
  private String status;

  @NotBlank(message = "Institution ID is required")
  private String institutionId;

  @Valid
  private List<ModuleRequest> modules;

  public CourseRequest() {
  }

  public CourseRequest(String title, String description, String status, String institutionId, List<ModuleRequest> modules) {
    this.title = title;
    this.description = description;
    this.status = status;
    this.institutionId = institutionId;
    this.modules = modules;
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

  public String getInstitutionId() {
    return institutionId;
  }

  public void setInstitutionId(String institutionId) {
    this.institutionId = institutionId;
  }

  public List<ModuleRequest> getModules() {
    return modules;
  }

  public void setModules(List<ModuleRequest> modules) {
    this.modules = modules;
  }
}
