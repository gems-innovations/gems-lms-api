package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ILessonRepository extends ReactiveCrudRepository<LessonEntity, Long> {
  Flux<LessonEntity> findByModuleId(Long moduleId);
}
