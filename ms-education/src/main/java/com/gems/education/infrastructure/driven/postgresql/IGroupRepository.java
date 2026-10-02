package com.gems.education.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface IGroupRepository extends ReactiveCrudRepository<GroupEntity, Long> {
  Flux<GroupEntity> findByInstitutionIdOrderByIdAsc(String institutionId);
}
