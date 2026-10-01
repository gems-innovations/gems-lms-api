package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Shared by create and update: title/institutionId are required for creation
 * (enforced in CreateCourseUseCase) but omitted on partial updates by the frontend's
 * IUpdateCourseRequest, so they can't be @NotBlank here without rejecting valid updates.
 */
public class CourseRequest {
  private String title;
  private String description;
  private String status;
  private String difficulty;
  private List<String> tags;
  private String thumbnailUrl;
  private String instructorName;
  private String institutionId;

  @Valid
  private List<ModuleRequest> modules;

  public CourseRequest() {
  }

  public CourseRequest(String title, String description, String status, String difficulty, List<String> tags,
                       String thumbnailUrl, String instructorName, String institutionId, List<ModuleRequest> modules) {
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

  public List<ModuleRequest> getModules() {
    return modules;
  }

  public void setModules(List<ModuleRequest> modules) {
    this.modules = modules;
  }
}
