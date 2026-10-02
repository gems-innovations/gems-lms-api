package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IQuizAttemptRepository extends ReactiveCrudRepository<QuizAttemptEntity, Long> {
  Flux<QuizAttemptEntity> findByStudentIdOrderByCompletedAtAsc(Long studentId);
  Mono<Long> countByEnrollmentIdAndBlockId(Long enrollmentId, Long blockId);
}
