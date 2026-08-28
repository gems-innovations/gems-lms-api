package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IEnrollmentRepository extends ReactiveCrudRepository<EnrollmentEntity, Long> {
  Flux<EnrollmentEntity> findByStudentId(Long studentId);
  Flux<EnrollmentEntity> findByCourseId(Long courseId);
  Mono<EnrollmentEntity> findByStudentIdAndCourseId(Long studentId, Long courseId);
  Mono<Boolean> existsByStudentIdAndCourseId(Long studentId, Long courseId);
}

