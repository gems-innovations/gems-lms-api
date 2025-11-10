package com.gems.education.application;


import com.gems.education.application.command.RegisterStudentCommand;
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

    public Mono<StudentResponse> execute(RegisterStudentCommand command) {
        return studentGateway.existsByEmail(new Email(command.getEmail()))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new StudentAlreadyExistsException(
                                String.format(StudentsConstants.STUDENT_ALREADY_EXISTS_MESSAGE, command.getEmail())
                        ));
                    }

                    // Crear entidad de dominio Student
                    Student student = new Student(
                            new Name(command.getName()),
                            new Email(command.getEmail()),
                            new BirthDate(command.getBirthDate()),
                            new Country(command.getCountry()),
                            new City(command.getCity()),
                            DocumentType.fromString(command.getDocumentType()),
                            new DocumentNumber(command.getDocumentNumber())
                    );

                    // Guardar y mapear a respuesta
                    return studentGateway.save(student)
                            .map(savedStudent -> new StudentResponse(
                                    savedStudent.getValue().getValue(),
                                    savedStudent.getName().getValue(),
                                    savedStudent.getEmail().getValue(),
                                    savedStudent.getBirthDate().getValue(),
                                    savedStudent.getCountry().getValue(),
                                    savedStudent.getCity().getValue(),
                                    savedStudent.getDocumentType().name(),
                                    savedStudent.getDocumentNumber().getValue()
                            ));
                });
    }

}
