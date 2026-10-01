package com.gems.education.application;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.response.CourseResponse;
import reactor.core.publisher.Flux;

public class GetCoursesByInstitutionUseCase {
  private final CourseGateway courseGateway;

  public GetCoursesByInstitutionUseCase(CourseGateway courseGateway) {
    this.courseGateway = courseGateway;
  }

  public Flux<CourseResponse> execute(String institutionId) {
    return courseGateway.findByInstitutionId(institutionId)
      .map(CourseResponseMapper::toResponse);
  }
}
