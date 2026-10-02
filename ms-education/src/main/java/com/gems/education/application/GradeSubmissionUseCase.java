package com.gems.education.application;

import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.domain.entities.AssignmentSubmission;
import reactor.core.publisher.Mono;

public class GradeSubmissionUseCase {
  private final CourseActivityGateway activityGateway;

  public GradeSubmissionUseCase(CourseActivityGateway activityGateway) {
    this.activityGateway = activityGateway;
  }

  public Mono<AssignmentSubmission> findSubmission(Long submissionId) {
    return activityGateway.findSubmission(submissionId)
      .switchIfEmpty(Mono.error(new CourseActivityException(
        CourseActivityException.SUBMISSION_NOT_FOUND, "Submission not found with ID " + submissionId)));
  }

  public Mono<AssignmentSubmission> execute(Long submissionId, int grade, String feedback) {
    if (grade < 0) {
      return Mono.error(new IllegalArgumentException("Grade cannot be negative"));
    }
    return findSubmission(submissionId)
      .flatMap(submission -> activityGateway.saveSubmission(submission.graded(grade, feedback)));
  }
}
