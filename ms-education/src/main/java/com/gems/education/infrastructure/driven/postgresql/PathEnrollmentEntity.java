package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("path_enrollments")
public class PathEnrollmentEntity {
  @Id
  private Long id;
  @Column("learning_path_id")
  private Long learningPathId;
  @Column("student_id")
  private Long studentId;
  private String status;
  @Column("enrolled_at")
  private LocalDateTime enrolledAt;
  @Column("completed_at")
  private LocalDateTime completedAt;

  public PathEnrollmentEntity() {
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getLearningPathId() { return learningPathId; }
  public void setLearningPathId(Long learningPathId) { this.learningPathId = learningPathId; }
  public Long getStudentId() { return studentId; }
  public void setStudentId(Long studentId) { this.studentId = studentId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getEnrolledAt() { return enrolledAt; }
  public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
  public LocalDateTime getCompletedAt() { return completedAt; }
  public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
