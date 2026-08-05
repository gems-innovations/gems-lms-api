package com.gems.education.application.command;

import java.util.List;

public class BulkEnrollmentCommand {
  private List<Long> studentIds;
  private Long courseId;

  public BulkEnrollmentCommand() {
  }

  public BulkEnrollmentCommand(List<Long> studentIds, Long courseId) {
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
