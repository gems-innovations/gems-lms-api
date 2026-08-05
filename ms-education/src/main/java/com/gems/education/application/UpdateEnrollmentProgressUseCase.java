package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.infrastructure.driving.rest.exeption.EnrollmentNotFoundException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class UpdateEnrollmentProgressUseCase {
  private final EnrollmentGateway enrollmentGateway;

  public UpdateEnrollmentProgressUseCase(EnrollmentGateway enrollmentGateway) {
    this.enrollmentGateway = enrollmentGateway;
  }

  public Mono<EnrollmentResponse> execute(Long enrollmentId, Integer progress) {
    if (progress < 0 || progress > 100) {
      return Mono.error(new IllegalArgumentException("Progress must be between 0 and 100"));
    }

    return enrollmentGateway.findById(enrollmentId)
      .switchIfEmpty(Mono.error(new EnrollmentNotFoundException("Enrollment not found with ID " + enrollmentId)))
      .flatMap(enrollment -> {
        enrollment.setProgress(progress);
        if (progress == 100) {
          enrollment.setCompletedAt(LocalDateTime.now());
        } else {
          enrollment.setCompletedAt(null);
        }
        return enrollmentGateway.save(enrollment);
      })
      .map(enrollment -> new EnrollmentResponse(
        enrollment.getId(),
        enrollment.getStudentId(),
        enrollment.getCourseId(),
        enrollment.getEnrolledAt(),
        enrollment.getProgress(),
        enrollment.getCompletedAt()
      ));
  }
}
