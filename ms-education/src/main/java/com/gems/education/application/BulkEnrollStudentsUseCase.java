package com.gems.education.application;

import com.gems.education.application.command.BulkEnrollmentCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.application.exceptions.CourseNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class BulkEnrollStudentsUseCase {
  private final EnrollmentGateway enrollmentGateway;
  private final CourseGateway courseGateway;
  private final EnrollmentRulesUseCase rules;

  public BulkEnrollStudentsUseCase(EnrollmentGateway enrollmentGateway, CourseGateway courseGateway,
                                   EnrollmentRulesUseCase rules) {
    this.enrollmentGateway = enrollmentGateway;
    this.courseGateway = courseGateway;
    this.rules = rules;
  }

  public Flux<EnrollmentResponse> execute(BulkEnrollmentCommand command) {
    return courseGateway.findById(command.getCourseId())
      .switchIfEmpty(Mono.error(new CourseNotFoundException("Course not found with ID " + command.getCourseId())))
      .flatMapMany(course -> Flux.fromIterable(command.getStudentIds())
        .concatMap(studentId -> enrollmentGateway.findByStudentIdAndCourseId(studentId, command.getCourseId())
          // Staff enroll: only the capacity applies. One at a time so the capacity holds within the batch.
          .switchIfEmpty(Mono.defer(() -> rules.requireAllowed(studentId, command.getCourseId(), true)
            .then(Mono.defer(() -> enrollmentGateway.saveRespectingCapacity(new Enrollment(null, studentId, command.getCourseId(),
              "active", LocalDateTime.now(), 0, null))))))
          .onErrorResume(e -> Mono.empty()) // Students that cannot be enrolled are skipped
        )
      )
      .map(this::mapToResponse);
  }

  private EnrollmentResponse mapToResponse(Enrollment enrollment) {
    return EnrollmentResponse.from(enrollment);
  }
}
