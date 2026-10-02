package com.gems.education.application;

import com.gems.education.application.gateway.PathEnrollmentGateway;
import com.gems.education.domain.entities.PathEnrollment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Enrollments in learning paths. Enrolling is idempotent: a student already in the path keeps
 * their enrollment. The path's courses are enrolled through the course enrollments API.
 */
public class PathEnrollmentUseCase {
  private final PathEnrollmentGateway gateway;

  public PathEnrollmentUseCase(PathEnrollmentGateway gateway) {
    this.gateway = gateway;
  }

  public Flux<PathEnrollment> enroll(Long learningPathId, List<Long> studentIds) {
    return Flux.fromIterable(studentIds.stream().filter(Objects::nonNull).distinct().toList())
      .concatMap(studentId -> gateway.find(learningPathId, studentId)
        .switchIfEmpty(Mono.defer(() -> gateway.save(new PathEnrollment(null, learningPathId, studentId,
          PathEnrollment.ACTIVE, LocalDateTime.now(), null)))));
  }

  public Flux<PathEnrollment> ofStudent(Long studentId) {
    return gateway.findByStudent(studentId);
  }

  public Flux<PathEnrollment> ofPath(Long learningPathId) {
    return gateway.findByPath(learningPathId);
  }
}
