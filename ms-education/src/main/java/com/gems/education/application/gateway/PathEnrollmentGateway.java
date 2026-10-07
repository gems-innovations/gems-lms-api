package com.gems.education.application.gateway;

import com.gems.education.domain.entities.PathEnrollment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PathEnrollmentGateway {
  Mono<PathEnrollment> save(PathEnrollment enrollment);
  Mono<PathEnrollment> find(Long learningPathId, Long studentId);
  Flux<PathEnrollment> findByStudent(Long studentId);
  Flux<PathEnrollment> findByPath(Long learningPathId);
}
