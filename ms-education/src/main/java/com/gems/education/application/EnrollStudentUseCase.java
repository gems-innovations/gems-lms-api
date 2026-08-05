package com.gems.education.application;

import com.gems.education.application.command.EnrollmentCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.values.StudentId;
import com.gems.education.infrastructure.driving.rest.exeption.CourseNotFoundException;
import com.gems.education.infrastructure.driving.rest.exeption.StudentNotFoundException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public class EnrollStudentUseCase {
  private final EnrollmentGateway enrollmentGateway;
  private final StudentGateway studentGateway;
  private final CourseGateway courseGateway;

  public EnrollStudentUseCase(EnrollmentGateway enrollmentGateway,
                              StudentGateway studentGateway,
                              CourseGateway courseGateway) {
    this.enrollmentGateway = enrollmentGateway;
    this.studentGateway = studentGateway;
    this.courseGateway = courseGateway;
  }

  public Mono<EnrollmentResponse> execute(EnrollmentCommand command) {
    StudentId sIdVo = new StudentId(command.getStudentId());
    return studentGateway.findById(sIdVo)
      .switchIfEmpty(Mono.error(new StudentNotFoundException("Student not found with ID " + command.getStudentId())))
      .flatMap(student -> courseGateway.findById(command.getCourseId())
        .switchIfEmpty(Mono.error(new CourseNotFoundException("Course not found with ID " + command.getCourseId())))
      )
      .flatMap(course -> enrollmentGateway.findByStudentIdAndCourseId(command.getStudentId(), command.getCourseId())
        .switchIfEmpty(Mono.defer(() -> {
          Enrollment enrollment = new Enrollment(null, command.getStudentId(), command.getCourseId(), LocalDateTime.now(), 0, null);
          return enrollmentGateway.save(enrollment);
        }))
      )
      .map(this::mapToResponse);
  }

  private EnrollmentResponse mapToResponse(Enrollment enrollment) {
    return new EnrollmentResponse(
      enrollment.getId(),
      enrollment.getStudentId(),
      enrollment.getCourseId(),
      enrollment.getEnrolledAt(),
      enrollment.getProgress(),
      enrollment.getCompletedAt()
    );
  }
}
