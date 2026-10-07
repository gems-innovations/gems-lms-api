package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.r2dbc.repository.Modifying;
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

  /** Recomputes the course's enrolled count and completion rate (% of enrollments completed). */
  @Modifying
  @Query("""
    UPDATE courses SET
        enrolled_count = (SELECT COUNT(*) FROM enrollments e WHERE e.course_id = courses.id),
        completion_rate = COALESCE((SELECT ROUND(100.0 * COUNT(*) FILTER (WHERE e.status = 'completed') / NULLIF(COUNT(*), 0))
                                    FROM enrollments e WHERE e.course_id = courses.id), 0)
    WHERE id = :courseId""")
  Mono<Integer> refreshCourseStats(Long courseId);
}
