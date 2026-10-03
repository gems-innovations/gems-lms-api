package com.gems.education.application;

import com.gems.education.application.command.EnrollmentCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.application.exceptions.CourseNotFoundException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Enrolls a user (identified by their ms-auth user id, passed here as "studentId") in a course.
 * Deliberately does not require a matching row in the local `students` table — that table is a
 * separate legacy student-profile registry (name, birth date, document, etc.) unrelated to the
 * ms-auth account actually doing the enrolling, and the frontend never populates it before
 * enrolling. Requiring it would make every enrollment fail with "student not found".
 */
public class EnrollStudentUseCase {
  private final EnrollmentGateway enrollmentGateway;
  private final CourseGateway courseGateway;

  public EnrollStudentUseCase(EnrollmentGateway enrollmentGateway, CourseGateway courseGateway) {
    this.enrollmentGateway = enrollmentGateway;
    this.courseGateway = courseGateway;
  }

  public Mono<EnrollmentResponse> execute(EnrollmentCommand command) {
    return courseGateway.findById(command.getCourseId())
      .switchIfEmpty(Mono.error(new CourseNotFoundException("Course not found with ID " + command.getCourseId())))
      .flatMap(course -> enrollmentGateway.findByStudentIdAndCourseId(command.getStudentId(), command.getCourseId())
        .switchIfEmpty(Mono.defer(() -> {
          Enrollment enrollment = new Enrollment(null, command.getStudentId(), command.getCourseId(), "active",
            LocalDateTime.now(), 0, null);
          return enrollmentGateway.save(enrollment);
        }))
      )
      .map(this::mapToResponse);
  }

  private EnrollmentResponse mapToResponse(Enrollment enrollment) {
    return EnrollmentResponse.from(enrollment);
  }
}
