package com.gems.education.application;

import com.gems.education.application.command.StudentCommand;
import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.StudentResponse;
import com.gems.education.domain.constants.StudentsConstants;
import com.gems.education.domain.entities.Student;
import com.gems.education.domain.values.*;
import com.gems.education.infrastructure.driving.rest.exeption.StudentAlreadyExistsException;
import reactor.core.publisher.Mono;

public class RegisterStudentUseCase {
  private final StudentGateway studentGateway;

  public RegisterStudentUseCase(StudentGateway studentGateway) {
    this.studentGateway = studentGateway;
  }

  public Mono<StudentResponse> execute(StudentCommand command) {
    return studentGateway.existsByEmail(new Email(command.getEmail()))
      .flatMap(emailExists -> {
        if (emailExists) {
          return Mono.error(new StudentAlreadyExistsException(
            String.format(StudentsConstants.STUDENT_ALREADY_EXISTS_EMAIL_MESSAGE, command.getEmail())
          ));
        }

        return studentGateway.existsByDocumentNumber(new DocumentNumber(command.getDocumentNumber()))
          .flatMap(documentExists -> {
            if (documentExists) {
              return Mono.error(new StudentAlreadyExistsException(
                String.format(StudentsConstants.STUDENT_ALREADY_EXISTS_DOCUMENT_MESSAGE, command.getDocumentNumber())
              ));
            }

            Student student = new Student(
              command.getName(),
              command.getEmail(),
              command.getBirthDate(),
              command.getCountry(),
              command.getCity(),
              command.getDocumentType(),
              command.getDocumentNumber()
            );

            return studentGateway.save(student)
              .map(savedStudent -> new StudentResponse(
                savedStudent.getId().getValue(),
                savedStudent.getName().getValue(),
                savedStudent.getEmail().getValue(),
                savedStudent.getBirthDate().getValue(),
                savedStudent.getCountry().getValue(),
                savedStudent.getCity().getValue(),
                savedStudent.getDocumentType().name(),
                savedStudent.getDocumentNumber().getValue()
              ));
          });
      });
  }

}
