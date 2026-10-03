package com.gems.education.application;

import com.gems.education.application.command.BulkEnrollmentCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.infrastructure.driving.rest.exeption.CourseNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class BulkEnrollStudentsUseCase {
  private final EnrollmentGateway enrollmentGateway;
  private final CourseGateway courseGateway;

  public BulkEnrollStudentsUseCase(EnrollmentGateway enrollmentGateway, CourseGateway courseGateway) {
    this.enrollmentGateway = enrollmentGateway;
    this.courseGateway = courseGateway;
  }

  public Flux<EnrollmentResponse> execute(BulkEnrollmentCommand command) {
    return courseGateway.findById(command.getCourseId())
      .switchIfEmpty(Mono.error(new CourseNotFoundException("Course not found with ID " + command.getCourseId())))
      .flatMapMany(course -> Flux.fromIterable(command.getStudentIds())
        .flatMap(studentId -> enrollmentGateway.findByStudentIdAndCourseId(studentId, command.getCourseId())
          .switchIfEmpty(Mono.defer(() -> {
            Enrollment enrollment = new Enrollment(null, studentId, command.getCourseId(), "active",
              LocalDateTime.now(), 0, null);
            return enrollmentGateway.save(enrollment);
          }))
          .onErrorResume(e -> Mono.empty()) // Resilient to individual student errors
        )
      )
      .map(this::mapToResponse);
  }

  private EnrollmentResponse mapToResponse(Enrollment enrollment) {
    return EnrollmentResponse.from(enrollment);
  }
}
