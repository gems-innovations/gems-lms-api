package com.gems.education.application.response;

import java.util.List;

public class ModuleResponse {
  private final Long id;
  private final Long courseId;
  private final String title;
  private final Integer orderIndex;
  private final List<LessonResponse> lessons;

  public ModuleResponse(Long id, Long courseId, String title, Integer orderIndex, List<LessonResponse> lessons) {
    this.id = id;
    this.courseId = courseId;
    this.title = title;
    this.orderIndex = orderIndex;
    this.lessons = lessons;
  }

  public Long getId() {
    return id;
  }

  public Long getCourseId() {
    return courseId;
  }

  public String getTitle() {
    return title;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public List<LessonResponse> getLessons() {
    return lessons;
  }
}
