package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class LessonRequest {
  // Present when updating an existing lesson; keeps its id stable across course updates.
  private Long id;

  @NotBlank(message = "Lesson title is required")
  private String title;

  @NotNull(message = "Lesson order index is required")
  private Integer orderIndex;

  @Valid
  private List<ContentRequest> contents;

  public LessonRequest() {
  }

  public LessonRequest(String title, Integer orderIndex, List<ContentRequest> contents) {
    this.title = title;
    this.orderIndex = orderIndex;
    this.contents = contents;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public void setOrderIndex(Integer orderIndex) {
    this.orderIndex = orderIndex;
  }

  public List<ContentRequest> getContents() {
    return contents;
  }

  public void setContents(List<ContentRequest> contents) {
    this.contents = contents;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  /** Optional lesson description; isFree lessons can be previewed without enrolling. */
  private String description;
  private Boolean isFree = false;

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Boolean getIsFree() { return isFree; }
  public void setIsFree(Boolean isFree) { this.isFree = Boolean.TRUE.equals(isFree); }

  public LessonRequest details(String description, Boolean isFree) {
    this.description = description;
    this.isFree = Boolean.TRUE.equals(isFree);
    return this;
  }
}
