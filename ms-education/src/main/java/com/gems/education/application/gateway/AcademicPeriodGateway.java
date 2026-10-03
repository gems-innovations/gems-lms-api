package com.gems.education.application.gateway;

import com.gems.education.domain.entities.AcademicPeriod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AcademicPeriodGateway {
  /** Newest first. */
  Flux<AcademicPeriod> findPeriodsOf(String institutionId);

  Mono<AcademicPeriod> findById(Long id);

  Mono<AcademicPeriod> save(AcademicPeriod period);

  Mono<Void> delete(Long id);
}
