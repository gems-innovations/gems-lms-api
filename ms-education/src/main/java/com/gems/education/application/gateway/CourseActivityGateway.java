package com.gems.education.application.gateway;

import com.gems.education.domain.entities.AssignmentSubmission;
import com.gems.education.domain.entities.QuizAttempt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Persistence of what students do inside a course: quiz attempts and assignment submissions. */
public interface CourseActivityGateway {
  Mono<QuizAttempt> saveAttempt(QuizAttempt attempt);
  Flux<QuizAttempt> findAttemptsByStudent(Long studentId);
  Mono<Long> countAttempts(Long enrollmentId, Long blockId);

  Mono<AssignmentSubmission> saveSubmission(AssignmentSubmission submission);
  Mono<AssignmentSubmission> findSubmission(Long id);
  Mono<AssignmentSubmission> findSubmission(Long enrollmentId, Long blockId);
  Flux<AssignmentSubmission> findSubmissionsByStudent(Long studentId);
  Flux<AssignmentSubmission> findSubmissionsByCourse(Long courseId);
  Flux<AssignmentSubmission> findSubmissionsByInstitution(String institutionId);
}
