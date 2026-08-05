package com.gems.education.application;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.infrastructure.driving.rest.exeption.CourseNotFoundException;
import reactor.core.publisher.Mono;

public class DeleteCourseUseCase {
  private final CourseGateway courseGateway;

  public DeleteCourseUseCase(CourseGateway courseGateway) {
    this.courseGateway = courseGateway;
  }

  public Mono<Void> execute(Long id) {
    return courseGateway.findById(id)
      .switchIfEmpty(Mono.error(new CourseNotFoundException("Course not found with ID " + id)))
      .flatMap(existing -> courseGateway.deleteById(id));
  }
}
