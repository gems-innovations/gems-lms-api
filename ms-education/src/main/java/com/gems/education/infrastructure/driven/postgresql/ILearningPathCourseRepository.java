package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ILearningPathCourseRepository extends ReactiveCrudRepository<LearningPathCourseEntity, Long> {
  Flux<LearningPathCourseEntity> findByLearningPathId(Long learningPathId);

  @Query("DELETE FROM learning_path_courses WHERE learning_path_id = :learningPathId")
  Mono<Void> deleteByLearningPathId(Long learningPathId);
}
