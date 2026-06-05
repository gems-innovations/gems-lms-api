package com.gems.education.application;


import com.gems.education.application.command.StudentCommand;
import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.StudentResponse;
import com.gems.education.domain.constants.StudentsConstants;
import com.gems.education.domain.entities.Student;
import com.gems.education.domain.values.*;
import com.gems.education.infrastructure.driving.rest.exeption.StudentNotFoundException;
import reactor.core.publisher.Mono;

public class UpdateStudentUseCase {
  private final StudentGateway studentGateway;

  public UpdateStudentUseCase(StudentGateway studentGateway) {
    this.studentGateway = studentGateway;
  }

  public Mono<StudentResponse> execute(Long id, StudentCommand command) {
    return studentGateway.findById(new StudentId(id))
      .switchIfEmpty(Mono.error(new StudentNotFoundException(
        String.format(StudentsConstants.STUDENT_NOT_FOUND_MESSAGE, id)
      )))
      .flatMap(existing -> {
        Student updated = new Student(
          existing.getId().getValue(),
          command.getName(),
          command.getEmail(),
          command.getBirthDate(),
          command.getCountry(),
          command.getCity(),
          command.getDocumentType(),
          command.getDocumentNumber()
        );
        return studentGateway.save(updated);
      })
      .map(updatedStudent -> new StudentResponse(
        updatedStudent.getId().getValue(),
        updatedStudent.getName().getValue(),
        updatedStudent.getEmail().getValue(),
        updatedStudent.getBirthDate().getValue(),
        updatedStudent.getCountry().getValue(),
        updatedStudent.getCity().getValue(),
        updatedStudent.getDocumentType().name(),
        updatedStudent.getDocumentNumber().getValue()
      ));
  }
}
