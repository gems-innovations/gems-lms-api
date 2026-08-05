package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ILearningPathRepository extends ReactiveCrudRepository<LearningPathEntity, Long> {
  Flux<LearningPathEntity> findByInstitutionId(String institutionId);
}
