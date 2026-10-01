package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import reactor.core.publisher.Flux;

/**
 * Lists enrollments for a given ms-auth user id ("studentId"). Deliberately does not require a
 * matching row in the local `students` table — see {@link EnrollStudentUseCase} for why.
 */
public class GetStudentEnrollmentsUseCase {
  private final EnrollmentGateway enrollmentGateway;

  public GetStudentEnrollmentsUseCase(EnrollmentGateway enrollmentGateway) {
    this.enrollmentGateway = enrollmentGateway;
  }

  public Flux<EnrollmentResponse> execute(Long studentId) {
    return enrollmentGateway.findByStudentId(studentId)
      .map(enrollment -> new EnrollmentResponse(
        enrollment.getId(),
        enrollment.getStudentId(),
        enrollment.getCourseId(),
        enrollment.getStatus(),
        enrollment.getEnrolledAt(),
        enrollment.getProgress(),
        enrollment.getCompletedAt()
      ));
  }
}
