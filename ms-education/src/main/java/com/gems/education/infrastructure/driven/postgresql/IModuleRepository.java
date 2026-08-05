package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface IModuleRepository extends ReactiveCrudRepository<ModuleEntity, Long> {
  Flux<ModuleEntity> findByCourseId(Long courseId);
}
