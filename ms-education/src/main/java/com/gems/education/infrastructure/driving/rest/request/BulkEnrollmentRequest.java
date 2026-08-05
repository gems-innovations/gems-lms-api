package com.gems.education.infrastructure.driving.rest.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class BulkEnrollmentRequest {
  @NotEmpty(message = "Student IDs cannot be empty")
  private List<Long> studentIds;

  @NotNull(message = "Course ID is required")
  private Long courseId;

  public BulkEnrollmentRequest() {
  }

  public BulkEnrollmentRequest(List<Long> studentIds, Long courseId) {
    this.studentIds = studentIds;
    this.courseId = courseId;
  }

  public List<Long> getStudentIds() {
    return studentIds;
  }

  public void setStudentIds(List<Long> studentIds) {
    this.studentIds = studentIds;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }
}
