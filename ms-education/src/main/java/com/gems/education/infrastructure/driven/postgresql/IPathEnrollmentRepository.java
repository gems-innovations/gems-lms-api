package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IPathEnrollmentRepository extends ReactiveCrudRepository<PathEnrollmentEntity, Long> {
  Flux<PathEnrollmentEntity> findByStudentIdOrderByEnrolledAtAsc(Long studentId);
  Flux<PathEnrollmentEntity> findByLearningPathIdOrderByEnrolledAtAsc(Long learningPathId);
  Mono<PathEnrollmentEntity> findByLearningPathIdAndStudentId(Long learningPathId, Long studentId);
  Mono<Long> countByLearningPathId(Long learningPathId);

  /** % of the path's students that completed every required course of the path. */
  @Query("""
    SELECT COALESCE(ROUND(100.0 * COUNT(*) FILTER (WHERE t.done) / NULLIF(COUNT(*), 0)), 0)::int
    FROM (
      SELECT NOT EXISTS (
        SELECT 1 FROM learning_path_courses lpc
        WHERE lpc.learning_path_id = pe.learning_path_id AND lpc.is_required
          AND NOT EXISTS (SELECT 1 FROM enrollments e
                          WHERE e.course_id = lpc.course_id AND e.student_id = pe.student_id
                            AND e.status = 'completed')
      ) AS done
      FROM path_enrollments pe WHERE pe.learning_path_id = :learningPathId
    ) t""")
  Mono<Integer> completionRate(Long learningPathId);
}
