package com.gems.education.application;

import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.StudentResponse;
import com.gems.education.domain.constants.StudentsConstants;
import com.gems.education.domain.values.StudentId;
import com.gems.education.infrastructure.driving.rest.exeption.StudentNotFoundException;
import reactor.core.publisher.Mono;

public class GetStudentByIdUseCase {
  private final StudentGateway studentGateway;

  public GetStudentByIdUseCase(StudentGateway studentGateway) {
    this.studentGateway = studentGateway;
  }

  public Mono<StudentResponse> execute(Long id) {
    return studentGateway.findById(new StudentId(id))
      .switchIfEmpty(Mono.error(new StudentNotFoundException(
        String.format(StudentsConstants.STUDENT_NOT_FOUND_MESSAGE, id)
      )))
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
