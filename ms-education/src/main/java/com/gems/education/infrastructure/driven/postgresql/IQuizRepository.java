package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface IQuizRepository extends ReactiveCrudRepository<QuizEntity, Long> {
  Mono<QuizEntity> findByLessonId(Long lessonId);
}
