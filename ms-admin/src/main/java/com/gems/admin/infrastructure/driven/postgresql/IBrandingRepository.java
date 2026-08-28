package com.gems.admin.infrastructure.driven.postgresql;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface IBrandingRepository extends ReactiveCrudRepository<BrandingEntity, Long> {
  Mono<BrandingEntity> findByCompanyId(String companyId);
  Mono<Void> deleteByCompanyId(String companyId);
}

