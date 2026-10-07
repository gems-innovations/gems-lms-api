package com.gems.education.application.response;

import com.gems.education.domain.entities.Enrollment;
import java.time.LocalDateTime;

public class EnrollmentResponse {
  private Long id;
  private Long studentId;
  private Long courseId;
  private String status;
  private LocalDateTime enrolledAt;
  private Integer progress;
  private LocalDateTime completedAt;

  // Detailed progress of the student (completed blocks, quiz attempts, submissions...) as JSON.
  private String progressData;

  public EnrollmentResponse() {
  }

  public EnrollmentResponse(Long id, Long studentId, Long courseId, String status, LocalDateTime enrolledAt,
                            Integer progress, LocalDateTime completedAt) {
    this.id = id;
    this.studentId = studentId;
    this.courseId = courseId;
    this.status = status;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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

  public String getProgressData() {
    return progressData;
  }

  public void setProgressData(String progressData) {
    this.progressData = progressData;
  }

  public static EnrollmentResponse from(Enrollment e) {
    EnrollmentResponse r = new EnrollmentResponse(e.getId(), e.getStudentId(), e.getCourseId(), e.getStatus(),
      e.getEnrolledAt(), e.getProgress(), e.getCompletedAt());
    r.setProgressData(e.getProgressData());
    return r;
  }
}
