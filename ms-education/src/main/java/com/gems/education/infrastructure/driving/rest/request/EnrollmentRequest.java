package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.constraints.NotNull;

public class EnrollmentRequest {
  @NotNull(message = "Student ID is required")
  private Long studentId;

  @NotNull(message = "Course ID is required")
  private Long courseId;

  public EnrollmentRequest() {
  }

  public EnrollmentRequest(Long studentId, Long courseId) {
    this.studentId = studentId;
    this.courseId = courseId;
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
}
