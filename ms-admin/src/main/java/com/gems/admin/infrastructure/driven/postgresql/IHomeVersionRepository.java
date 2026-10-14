package com.gems.admin.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IHomeVersionRepository extends ReactiveCrudRepository<HomeVersionEntity, Long> {
  Flux<HomeVersionEntity> findByInstitutionId(String institutionId);

  Mono<HomeVersionEntity> findByInstitutionIdAndVersion(String institutionId, Integer version);
}
