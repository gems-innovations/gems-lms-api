package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import reactor.core.publisher.Flux;

/** Every enrollment in the courses of an institution (staff dashboards), in one query. */
public class GetEnrollmentsByInstitutionUseCase {
  private final EnrollmentGateway enrollmentGateway;

  public GetEnrollmentsByInstitutionUseCase(EnrollmentGateway enrollmentGateway) {
    this.enrollmentGateway = enrollmentGateway;
  }

  public Flux<EnrollmentResponse> execute(String institutionId) {
    return enrollmentGateway.findByInstitutionId(institutionId).map(EnrollmentResponse::from);
  }
}
