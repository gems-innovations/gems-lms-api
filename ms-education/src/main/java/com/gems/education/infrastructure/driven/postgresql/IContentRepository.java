package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface IContentRepository extends ReactiveCrudRepository<ContentEntity, Long> {
  Flux<ContentEntity> findByLessonId(Long lessonId);
}
