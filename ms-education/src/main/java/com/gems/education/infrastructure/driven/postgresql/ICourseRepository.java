package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ICourseRepository extends ReactiveCrudRepository<CourseEntity, Long> {
  Flux<CourseEntity> findByInstitutionId(String institutionId);
}
