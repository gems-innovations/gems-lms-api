package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IQuestionRepository extends ReactiveCrudRepository<QuestionEntity, Long> {
  Flux<QuestionEntity> findByQuizId(Long quizId);

  @Query("DELETE FROM questions WHERE quiz_id = :quizId")
  Mono<Void> deleteByQuizId(Long quizId);
}
