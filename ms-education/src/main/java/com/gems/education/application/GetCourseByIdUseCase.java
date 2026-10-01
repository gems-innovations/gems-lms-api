package com.gems.education.application;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.infrastructure.driving.rest.exeption.CourseNotFoundException;
import reactor.core.publisher.Mono;

public class GetCourseByIdUseCase {
  private final CourseGateway courseGateway;

  public GetCourseByIdUseCase(CourseGateway courseGateway) {
    this.courseGateway = courseGateway;
  }

  public Mono<CourseResponse> execute(Long id) {
    return courseGateway.findById(id)
      .map(CourseResponseMapper::toResponse)
      .switchIfEmpty(Mono.error(new CourseNotFoundException("Course not found with ID " + id)));
  }
}
