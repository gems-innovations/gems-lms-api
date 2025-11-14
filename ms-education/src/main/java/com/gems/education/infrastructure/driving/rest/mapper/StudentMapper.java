package com.gems.education.infrastructure.driving.rest.mapper;

import com.gems.education.application.command.StudentCommand;
import com.gems.education.infrastructure.driving.rest.request.StudentRequest;


public class StudentMapper {
  private StudentMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static StudentCommand toDomain(StudentRequest request) {
    return new StudentCommand(
      request.getName(),
      request.getEmail(),
      request.getBirthDate(),
      request.getCountry(),
      request.getCity(),
      request.getDocumentType(),
      request.getDocumentNumber()
    );
  }
}
