package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("learning_path_courses")
public class LearningPathCourseEntity implements Persistable<Long> {
  @Id
  @Column("learning_path_id")
  private Long learningPathId;

  @Column("course_id")
  private Long courseId;

  @Column("order_index")
  private Integer orderIndex;

  @Transient
  private boolean isNewEntry = true;

  public LearningPathCourseEntity() {
  }

  public LearningPathCourseEntity(Long learningPathId, Long courseId, Integer orderIndex) {
    this.learningPathId = learningPathId;
    this.courseId = courseId;
    this.orderIndex = orderIndex;
  }

  public Long getLearningPathId() {
    return learningPathId;
  }

  public void setLearningPathId(Long learningPathId) {
    this.learningPathId = learningPathId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public void setOrderIndex(Integer orderIndex) {
    this.orderIndex = orderIndex;
  }

  @Override
  public Long getId() {
    return learningPathId;
  }

  @Override
  public boolean isNew() {
    return isNewEntry;
  }

  public void setNew(boolean isNewEntry) {
    this.isNewEntry = isNewEntry;
  }
}
