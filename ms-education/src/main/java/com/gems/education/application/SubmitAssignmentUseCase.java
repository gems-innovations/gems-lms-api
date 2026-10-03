package com.gems.education.application;

import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.application.gateway.ContentBlockGateway;
import com.gems.education.application.gateway.CourseActivityGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.domain.entities.AssignmentSubmission;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static com.gems.education.application.exceptions.CourseActivityException.*;

/** Records (or replaces) a student's delivery for an assignment block; it goes back to pending. */
public class SubmitAssignmentUseCase {
  private static final String ASSIGNMENT = "assignment";

  private final EnrollmentGateway enrollmentGateway;
  private final CourseActivityGateway activityGateway;
  private final ContentBlockGateway contentBlockGateway;

  public SubmitAssignmentUseCase(EnrollmentGateway enrollmentGateway, CourseActivityGateway activityGateway,
                                 ContentBlockGateway contentBlockGateway) {
    this.enrollmentGateway = enrollmentGateway;
    this.activityGateway = activityGateway;
    this.contentBlockGateway = contentBlockGateway;
  }

  public Mono<AssignmentSubmission> execute(Long studentId, Long courseId, Long blockId, String textContent,
                                            String fileUrls) {
    return enrollmentGateway.findByStudentIdAndCourseId(studentId, courseId)
      .switchIfEmpty(Mono.error(new CourseActivityException(NOT_ENROLLED, "You are not enrolled in this course")))
      .flatMap(enrollment -> contentBlockGateway.find(courseId, blockId)
        .switchIfEmpty(Mono.error(new CourseActivityException(BLOCK_NOT_FOUND, "Content block not found in this course")))
        .flatMap(block -> {
          if (!ASSIGNMENT.equals(block.type())) {
            return Mono.error(new CourseActivityException(WRONG_BLOCK_TYPE, "This content block is not an assignment"));
          }
          return activityGateway.findSubmission(enrollment.getId(), blockId)
            .map(AssignmentSubmission::id)
            .defaultIfEmpty(-1L)
            .flatMap(existingId -> activityGateway.saveSubmission(new AssignmentSubmission(
              existingId > 0 ? existingId : null, enrollment.getId(), studentId, courseId, blockId,
              block.lessonId(), textContent, fileUrls, LocalDateTime.now(), null, null,
              AssignmentSubmission.PENDING, null)));
        }));
  }
}
