package com.gems.education.application;

import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.domain.entities.AssignmentSubmission;
import com.gems.education.domain.entities.QuizAttempt;
import reactor.core.publisher.Flux;

/** Read side of quiz attempts and assignment submissions. */
public class GetCourseActivityUseCase {
  private final CourseActivityGateway activityGateway;

  public GetCourseActivityUseCase(CourseActivityGateway activityGateway) {
    this.activityGateway = activityGateway;
  }

  public Flux<QuizAttempt> attemptsOfStudent(Long studentId) {
    return activityGateway.findAttemptsByStudent(studentId);
  }

  public Flux<AssignmentSubmission> submissionsOfStudent(Long studentId) {
    return activityGateway.findSubmissionsByStudent(studentId);
  }

  public Flux<AssignmentSubmission> submissionsOfCourse(Long courseId) {
    return activityGateway.findSubmissionsByCourse(courseId);
  }

  public Flux<AssignmentSubmission> submissionsOfInstitution(String institutionId) {
    return activityGateway.findSubmissionsByInstitution(institutionId);
  }
}
