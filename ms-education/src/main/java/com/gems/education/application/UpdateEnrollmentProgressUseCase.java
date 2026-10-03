package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.application.exceptions.EnrollmentNotFoundException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class UpdateEnrollmentProgressUseCase {
  private final EnrollmentGateway enrollmentGateway;

  public UpdateEnrollmentProgressUseCase(EnrollmentGateway enrollmentGateway) {
    this.enrollmentGateway = enrollmentGateway;
  }

  public Mono<EnrollmentResponse> execute(Long enrollmentId, Integer progress) {
    return execute(enrollmentId, progress, null);
  }

  /** Updates the percentage and, when given, the detailed progress JSON (null keeps the current one). */
  public Mono<EnrollmentResponse> execute(Long enrollmentId, Integer progress, String progressData) {
    if (progress == null || progress < 0 || progress > 100) {
      return Mono.error(new IllegalArgumentException("Progress must be between 0 and 100"));
    }

    return enrollmentGateway.findById(enrollmentId)
      .switchIfEmpty(Mono.error(new EnrollmentNotFoundException("Enrollment not found with ID " + enrollmentId)))
      .flatMap(enrollment -> {
        enrollment.setProgress(progress);
        if (progressData != null) enrollment.setProgressData(progressData);
        if (progress == 100) {
          enrollment.setCompletedAt(LocalDateTime.now());
          enrollment.setStatus("completed");
        } else {
          enrollment.setCompletedAt(null);
          if ("completed".equals(enrollment.getStatus())) {
            enrollment.setStatus("active");
          }
        }
        return enrollmentGateway.save(enrollment);
      })
      .map(enrollment -> EnrollmentResponse.from(enrollment));
  }
}
