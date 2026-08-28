package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import reactor.core.publisher.Flux;

public class GetEnrollmentsByCourseUseCase {
  private final EnrollmentGateway enrollmentGateway;

  public GetEnrollmentsByCourseUseCase(EnrollmentGateway enrollmentGateway) {
    this.enrollmentGateway = enrollmentGateway;
  }

  public Flux<EnrollmentResponse> execute(Long courseId) {
    return enrollmentGateway.findByCourseId(courseId)
      .map(e -> new EnrollmentResponse(
        e.getId(), e.getStudentId(), e.getCourseId(),
        e.getEnrolledAt(), e.getProgress(), e.getCompletedAt()
      ));
  }
}
