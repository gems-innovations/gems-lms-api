package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.infrastructure.driving.rest.exeption.EnrollmentNotFoundException;
import reactor.core.publisher.Mono;

public class DeleteEnrollmentUseCase {
  private final EnrollmentGateway enrollmentGateway;

  public DeleteEnrollmentUseCase(EnrollmentGateway enrollmentGateway) {
    this.enrollmentGateway = enrollmentGateway;
  }

  public Mono<Void> execute(Long id) {
    return enrollmentGateway.findById(id)
      .switchIfEmpty(Mono.error(new EnrollmentNotFoundException("Enrollment not found with ID: " + id)))
      .flatMap(enrollment -> enrollmentGateway.deleteById(enrollment.getId()));
  }
}
