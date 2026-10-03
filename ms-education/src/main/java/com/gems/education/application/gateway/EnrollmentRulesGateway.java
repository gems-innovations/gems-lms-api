package com.gems.education.application.gateway;

import com.gems.education.domain.entities.EnrollmentRules;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface EnrollmentRulesGateway {
  /** Empty when the course has no rules. */
  Mono<EnrollmentRules> find(Long courseId);

  Flux<EnrollmentRules> findAll(Collection<Long> courseIds);

  /** Rules of every course of the institution (to detect prerequisite cycles). */
  Flux<EnrollmentRules> findByInstitution(String institutionId);

  Mono<EnrollmentRules> save(EnrollmentRules rules);
}
