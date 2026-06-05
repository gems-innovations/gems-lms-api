package com.gems.education.application;

import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.domain.constants.StudentsConstants;
import com.gems.education.infrastructure.driving.rest.exeption.StudentNotFoundException;
import com.gems.education.domain.values.StudentId;
import reactor.core.publisher.Mono;

public class DeleteStudentUseCase {
  private final StudentGateway studentGateway;

  public DeleteStudentUseCase(StudentGateway studentGateway) {
    this.studentGateway = studentGateway;
  }

  public Mono<Void> execute(Long id) {
    return studentGateway.findById(new StudentId(id))
      .switchIfEmpty(Mono.error(new StudentNotFoundException(
        String.format(StudentsConstants.STUDENT_NOT_FOUND_MESSAGE, id)
      )))
      .flatMap(student -> studentGateway.deleteById(new StudentId(id)));
  }
}