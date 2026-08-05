package com.gems.education.application.gateway;

import com.gems.education.domain.entities.Quiz;
import reactor.core.publisher.Mono;

public interface QuizGateway {
  Mono<Quiz> save(Quiz quiz);
  Mono<Quiz> findById(Long id);
  Mono<Quiz> findByLessonId(Long lessonId);
  Mono<Void> deleteById(Long id);
}
