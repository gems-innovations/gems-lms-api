package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IEnrollmentRepository extends ReactiveCrudRepository<EnrollmentEntity, Long> {
  Flux<EnrollmentEntity> findByStudentId(Long studentId);
  Flux<EnrollmentEntity> findByCourseId(Long courseId);

  @Query("SELECT e.* FROM enrollments e JOIN courses c ON c.id = e.course_id WHERE c.institution_id = :institutionId")
  Flux<EnrollmentEntity> findByInstitutionId(String institutionId);
  Mono<EnrollmentEntity> findByStudentIdAndCourseId(Long studentId, Long courseId);
  Mono<Boolean> existsByStudentIdAndCourseId(Long studentId, Long courseId);
}

