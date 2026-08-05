package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("modules")
public class ModuleEntity {
  @Id
  private Long id;

  @Column("course_id")
  private Long courseId;

  private String title;

  @Column("order_index")
  private Integer orderIndex;

  @Column("created_at")
  private LocalDateTime createdAt;

  public ModuleEntity() {
  }

  public ModuleEntity(Long id, Long courseId, String title, Integer orderIndex, LocalDateTime createdAt) {
    this.id = id;
    this.courseId = courseId;
    this.title = title;
    this.orderIndex = orderIndex;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
