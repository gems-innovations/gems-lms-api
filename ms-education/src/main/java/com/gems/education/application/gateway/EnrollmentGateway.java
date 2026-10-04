package com.gems.education.application.gateway;

import com.gems.education.domain.entities.Enrollment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EnrollmentGateway {
  Mono<Enrollment> save(Enrollment enrollment);
  /** Atomically reserves a seat and saves; empty means the course reached its capacity. */
  default Mono<Enrollment> saveRespectingCapacity(Enrollment enrollment) { return save(enrollment); }
  Mono<Enrollment> findById(Long id);
  Flux<Enrollment> findByStudentId(Long studentId);
  Flux<Enrollment> findByCourseId(Long courseId);
  Flux<Enrollment> findByInstitutionId(String institutionId);
  Mono<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);
  Mono<Boolean> existsByStudentIdAndCourseId(Long studentId, Long courseId);
  Mono<Void> deleteById(Long id);
}

