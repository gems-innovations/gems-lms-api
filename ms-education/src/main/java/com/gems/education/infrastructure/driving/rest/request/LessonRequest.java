package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class LessonRequest {
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
}
