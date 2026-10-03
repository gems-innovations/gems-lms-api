package com.gems.education.application.response;

import java.util.List;

public class LessonResponse {
  private final Long id;
  private final Long moduleId;
  private final String title;
  private final Integer orderIndex;
  private final List<ContentResponse> contents;

  public LessonResponse(Long id, Long moduleId, String title, Integer orderIndex, List<ContentResponse> contents) {
    this.id = id;
    this.moduleId = moduleId;
    this.title = title;
    this.orderIndex = orderIndex;
    this.contents = contents;
  }

  public Long getId() {
    return id;
  }

  public Long getModuleId() {
    return moduleId;
  }

  public String getTitle() {
    return title;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public List<ContentResponse> getContents() {
    return contents;
  }

  /** Optional lesson description; isFree lessons can be previewed without enrolling. */
  private String description;
  private Boolean isFree = false;

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Boolean getIsFree() { return isFree; }
  public void setIsFree(Boolean isFree) { this.isFree = Boolean.TRUE.equals(isFree); }

  public LessonResponse details(String description, Boolean isFree) {
    this.description = description;
    this.isFree = Boolean.TRUE.equals(isFree);
    return this;
  }
}
