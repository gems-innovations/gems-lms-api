package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IAssignmentSubmissionRepository extends ReactiveCrudRepository<AssignmentSubmissionEntity, Long> {
  Mono<AssignmentSubmissionEntity> findByEnrollmentIdAndBlockId(Long enrollmentId, Long blockId);
  Flux<AssignmentSubmissionEntity> findByStudentId(Long studentId);
  Flux<AssignmentSubmissionEntity> findByCourseIdOrderBySubmittedAtDesc(Long courseId);

  @Query("SELECT s.* FROM assignment_submissions s JOIN courses c ON c.id = s.course_id "
    + "WHERE c.institution_id = :institutionId ORDER BY s.submitted_at DESC")
  Flux<AssignmentSubmissionEntity> findByInstitutionId(String institutionId);
}
