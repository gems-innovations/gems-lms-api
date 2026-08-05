package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("contents")
public class ContentEntity {
  @Id
  private Long id;

  @Column("lesson_id")
  private Long lessonId;

  private String type;
  private String value;

  @Column("order_index")
  private Integer orderIndex;

  public ContentEntity() {
  }

  public ContentEntity(Long id, Long lessonId, String type, String value, Integer orderIndex) {
    this.id = id;
    this.lessonId = lessonId;
    this.type = type;
    this.value = value;
    this.orderIndex = orderIndex;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getLessonId() {
    return lessonId;
  }

  public void setLessonId(Long lessonId) {
    this.lessonId = lessonId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public void setOrderIndex(Integer orderIndex) {
    this.orderIndex = orderIndex;
  }
}
