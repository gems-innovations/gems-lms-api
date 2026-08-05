package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("enrollments")
public class EnrollmentEntity {
  @Id
  private Long id;

  @Column("student_id")
  private Long studentId;

  @Column("course_id")
  private Long courseId;

  @Column("enrolled_at")
  private LocalDateTime enrolledAt;

  private Integer progress;

  @Column("completed_at")
  private LocalDateTime completedAt;

  public EnrollmentEntity() {
  }

  public EnrollmentEntity(Long id, Long studentId, Long courseId, LocalDateTime enrolledAt, Integer progress, LocalDateTime completedAt) {
    this.id = id;
    this.studentId = studentId;
    this.courseId = courseId;
    this.enrolledAt = enrolledAt;
    this.progress = progress;
    this.completedAt = completedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getStudentId() {
    return studentId;
  }

  public void setStudentId(Long studentId) {
    this.studentId = studentId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public LocalDateTime getEnrolledAt() {
    return enrolledAt;
  }

  public void setEnrolledAt(LocalDateTime enrolledAt) {
    this.enrolledAt = enrolledAt;
  }

  public Integer getProgress() {
    return progress;
  }

  public void setProgress(Integer progress) {
    this.progress = progress;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }
}
