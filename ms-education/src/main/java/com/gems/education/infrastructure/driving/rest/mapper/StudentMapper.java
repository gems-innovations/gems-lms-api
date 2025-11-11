package com.gems.education.infrastructure.driving.rest.mapper;

import com.gems.education.application.command.RegisterStudentCommand;
import com.gems.education.infrastructure.driving.rest.request.RegisterStudentRequest;

public class StudentMapper {
  private StudentMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static RegisterStudentCommand toDomain(RegisterStudentRequest request) {
    return new RegisterStudentCommand(
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
