package com.gems.education.application;

import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.StudentResponse;
import reactor.core.publisher.Flux;

public class GetAllStudentsUseCase {
  private final StudentGateway studentGateway;

  public GetAllStudentsUseCase(StudentGateway studentGateway) {
    this.studentGateway = studentGateway;
  }

  public Flux<StudentResponse> execute() {
    return studentGateway.findAll()
      .map(student -> new StudentResponse(
        student.getId().getValue(),
        student.getName().getValue(),
        student.getEmail().getValue(),
        student.getBirthDate().getValue(),
        student.getCountry().getValue(),
        student.getCity().getValue(),
        student.getDocumentType().name(),
        student.getDocumentNumber().getValue()
      ));
  }
}
