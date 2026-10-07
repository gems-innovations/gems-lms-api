package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.application.exceptions.EnrollmentNotFoundException;
import reactor.core.publisher.Mono;

public class GetEnrollmentByIdUseCase {
  private final EnrollmentGateway enrollmentGateway;

  public GetEnrollmentByIdUseCase(EnrollmentGateway enrollmentGateway) {
    this.enrollmentGateway = enrollmentGateway;
  }

  public Mono<EnrollmentResponse> execute(Long id) {
    return enrollmentGateway.findById(id)
      .switchIfEmpty(Mono.error(new EnrollmentNotFoundException("Enrollment not found with ID: " + id)))
      .map(e -> EnrollmentResponse.from(e));
  }
}
