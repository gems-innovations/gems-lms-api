package com.gems.education.application.response;

public class ContentResponse {
  private final Long id;
  private final Long lessonId;
  private final String type;
  private final String value;
  private final Integer orderIndex;

  public ContentResponse(Long id, Long lessonId, String type, String value, Integer orderIndex) {
    this.id = id;
    this.lessonId = lessonId;
    this.type = type;
    this.value = value;
    this.orderIndex = orderIndex;
  }

  public Long getId() {
    return id;
  }

  public Long getLessonId() {
    return lessonId;
  }

  public String getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }
}
