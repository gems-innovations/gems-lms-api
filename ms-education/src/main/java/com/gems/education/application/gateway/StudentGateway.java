package com.gems.education.application.gateway;

import com.gems.education.domain.entities.Student;
import com.gems.education.domain.values.Email;
import com.gems.education.domain.values.StudentId;
import reactor.core.publisher.Mono;

public interface StudentGateway {
    Mono<Student> save(Student student);
    Mono<Student> findById(StudentId id);
    Mono<Student> findByEmail(Email email);
    Mono<Boolean> existsByEmail(Email email);
    Mono<Void> deleteById(StudentId id);
}
