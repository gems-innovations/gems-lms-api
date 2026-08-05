package com.gems.education.application.gateway;

import com.gems.education.domain.entities.Enrollment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EnrollmentGateway {
  Mono<Enrollment> save(Enrollment enrollment);
  Mono<Enrollment> findById(Long id);
  Flux<Enrollment> findByStudentId(Long studentId);
  Mono<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
  Mono<Boolean> existsByStudentIdAndCourseId(Long studentId, Long courseId);
  Mono<Void> deleteById(Long id);
}
