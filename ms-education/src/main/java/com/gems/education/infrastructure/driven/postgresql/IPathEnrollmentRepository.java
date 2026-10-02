package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IPathEnrollmentRepository extends ReactiveCrudRepository<PathEnrollmentEntity, Long> {
  Flux<PathEnrollmentEntity> findByStudentIdOrderByEnrolledAtAsc(Long studentId);
  Flux<PathEnrollmentEntity> findByLearningPathIdOrderByEnrolledAtAsc(Long learningPathId);
  Mono<PathEnrollmentEntity> findByLearningPathIdAndStudentId(Long learningPathId, Long studentId);
  Mono<Long> countByLearningPathId(Long learningPathId);
}
