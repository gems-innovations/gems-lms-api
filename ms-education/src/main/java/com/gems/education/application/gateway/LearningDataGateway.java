package com.gems.education.application.gateway;

import reactor.core.publisher.Mono;

/** Everything a student did in an institution (or everywhere when institutionId is null). */
public interface LearningDataGateway {
  /** Removes it and refreshes the course counters and ratings it affected. */
  Mono<Void> purge(Long studentId, String institutionId);
}
