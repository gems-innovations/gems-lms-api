package com.gems.education.application;

import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.application.gateway.ContentBlockGateway;
import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.domain.entities.QuizAttempt;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static com.gems.education.application.exceptions.CourseActivityException.*;

/** Grades a student's answers to a quiz block on the server and records the attempt. */
public class SubmitQuizAttemptUseCase {
  private static final String QUIZ = "quiz";

  private final EnrollmentGateway enrollmentGateway;
  private final CourseActivityGateway activityGateway;
  private final ContentBlockGateway contentBlockGateway;

  public SubmitQuizAttemptUseCase(EnrollmentGateway enrollmentGateway, CourseActivityGateway activityGateway,
                                  ContentBlockGateway contentBlockGateway) {
    this.enrollmentGateway = enrollmentGateway;
    this.activityGateway = activityGateway;
    this.contentBlockGateway = contentBlockGateway;
  }

  public Mono<QuizAttempt> execute(Long studentId, Long courseId, Long blockId, String answers) {
    return enrollmentGateway.findByStudentIdAndCourseId(studentId, courseId)
      .switchIfEmpty(Mono.error(new CourseActivityException(NOT_ENROLLED, "You are not enrolled in this course")))
      .flatMap(enrollment -> contentBlockGateway.find(courseId, blockId)
        .switchIfEmpty(Mono.error(new CourseActivityException(BLOCK_NOT_FOUND, "Content block not found in this course")))
        .flatMap(block -> {
          if (!QUIZ.equals(block.type())) {
            return Mono.error(new CourseActivityException(WRONG_BLOCK_TYPE, "This content block is not a quiz"));
          }
          return activityGateway.countAttempts(enrollment.getId(), blockId).flatMap(previous -> {
            if (block.maxAttempts() > 0 && previous >= block.maxAttempts()) {
              return Mono.error(new CourseActivityException(ATTEMPT_LIMIT_REACHED, "No attempts left for this quiz"));
            }
            return contentBlockGateway.grade(courseId, blockId, answers)
              .flatMap(grading -> activityGateway.saveAttempt(new QuizAttempt(
                null, enrollment.getId(), studentId, courseId, blockId, block.lessonId(),
                previous.intValue() + 1, answers, grading.score(), grading.passed(), grading.feedback(),
                LocalDateTime.now())));
          });
        }));
  }
}
