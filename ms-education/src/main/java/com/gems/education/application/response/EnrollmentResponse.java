package com.gems.education.application.response;

import java.time.LocalDateTime;

public class EnrollmentResponse {
  private Long id;
  private Long studentId;
  private Long courseId;
  private LocalDateTime enrolledAt;
  private Integer progress;
  private LocalDateTime completedAt;

  public EnrollmentResponse() {
  }

  public EnrollmentResponse(Long id, Long studentId, Long courseId, LocalDateTime enrolledAt, Integer progress, LocalDateTime completedAt) {
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
