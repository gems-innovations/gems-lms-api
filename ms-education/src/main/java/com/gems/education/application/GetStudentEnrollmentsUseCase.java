package com.gems.education.application;

import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.EnrollmentResponse;
import com.gems.education.domain.values.StudentId;
import com.gems.education.infrastructure.driving.rest.exeption.StudentNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GetStudentEnrollmentsUseCase {
  private final EnrollmentGateway enrollmentGateway;
  private final StudentGateway studentGateway;

  public GetStudentEnrollmentsUseCase(EnrollmentGateway enrollmentGateway, StudentGateway studentGateway) {
    this.enrollmentGateway = enrollmentGateway;
    this.studentGateway = studentGateway;
  }

  public Flux<EnrollmentResponse> execute(Long studentId) {
    StudentId sIdVo = new StudentId(studentId);
    return studentGateway.findById(sIdVo)
      .switchIfEmpty(Mono.error(new StudentNotFoundException("Student not found with ID " + studentId)))
      .flatMapMany(student -> enrollmentGateway.findByStudentId(studentId))
      .map(enrollment -> new EnrollmentResponse(
        enrollment.getId(),
        enrollment.getStudentId(),
        enrollment.getCourseId(),
        enrollment.getEnrolledAt(),
        enrollment.getProgress(),
        enrollment.getCompletedAt()
      ));
  }
}
