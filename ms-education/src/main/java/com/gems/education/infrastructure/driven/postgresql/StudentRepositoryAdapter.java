package com.gems.education.infrastructure.driven.postgresql;

import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.domain.entities.Student;
import com.gems.education.domain.values.BirthDate;
import com.gems.education.domain.values.City;
import com.gems.education.domain.values.Country;
import com.gems.education.domain.values.DocumentNumber;
import com.gems.education.domain.values.DocumentType;
import com.gems.education.domain.values.Email;
import com.gems.education.domain.values.Name;
import com.gems.education.domain.values.StudentId;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class StudentRepositoryAdapter implements StudentGateway {

    private final IStudentRepository studentRepository;

    public StudentRepositoryAdapter(IStudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public Mono<Student> save(Student student) {
        StudentEntity entity = mapToEntity(student);
        return studentRepository.save(entity)
                .map(this::mapToDomain);
    }

    @Override
    public Mono<Student> findById(StudentId id) {
        return studentRepository.findById(id.getValue())
                .map(this::mapToDomain);
    }

    @Override
    public Mono<Student> findByEmail(Email email) {
        return studentRepository.findByEmail(email.getValue())
                .map(this::mapToDomain);
    }

    @Override
    public Mono<Boolean> existsByEmail(Email email) {
        return studentRepository.existsByEmail(email.getValue());
    }

    @Override
    public Mono<Void> deleteById(StudentId id) {
        return studentRepository.deleteById(id.getValue());
    }

    private Student mapToDomain(StudentEntity entity) {
        return new Student(
                new StudentId(entity.getId()),
                new Name(entity.getName()),
                new Email(entity.getEmail()),
                new BirthDate(entity.getBirthDate()),
                new Country(entity.getCountry()),
                new City(entity.getCity()),
                DocumentType.valueOf(entity.getDocumentType()),
                new DocumentNumber(entity.getDocumentNumber())
        );
    }

    private StudentEntity mapToEntity(Student student) {
        return new StudentEntity(
                student.getValue() != null ? student.getValue().getValue() : null,
                student.getName().getValue(),
                student.getEmail().getValue(),
                student.getBirthDate().getValue(),
                student.getCountry().getValue(),
                student.getCity().getValue(),
                student.getDocumentType().name(),
                student.getDocumentNumber().getValue()
        );
    }
}
