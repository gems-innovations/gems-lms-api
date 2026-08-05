package com.gems.education.infrastructure.driving.rest.mapper;

import com.gems.education.application.command.BulkEnrollmentCommand;
import com.gems.education.application.command.EnrollmentCommand;
import com.gems.education.infrastructure.driving.rest.request.BulkEnrollmentRequest;
import com.gems.education.infrastructure.driving.rest.request.EnrollmentRequest;

public class EnrollmentMapper {
  private EnrollmentMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static EnrollmentCommand toCommand(EnrollmentRequest request) {
    return new EnrollmentCommand(request.getStudentId(), request.getCourseId());
  }

  public static BulkEnrollmentCommand toCommand(BulkEnrollmentRequest request) {
    return new BulkEnrollmentCommand(request.getStudentIds(), request.getCourseId());
  }
}
