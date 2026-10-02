package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ModuleRequest {
  // Present when updating an existing module; keeps its id stable across course updates.
  private Long id;

  @NotBlank(message = "Module title is required")
  private String title;

  @NotNull(message = "Module order index is required")
  private Integer orderIndex;

  @Valid
  private List<LessonRequest> lessons;

  public ModuleRequest() {
  }

  public ModuleRequest(String title, Integer orderIndex, List<LessonRequest> lessons) {
    this.title = title;
    this.orderIndex = orderIndex;
    this.lessons = lessons;
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

  public List<LessonRequest> getLessons() {
    return lessons;
  }

  public void setLessons(List<LessonRequest> lessons) {
    this.lessons = lessons;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
