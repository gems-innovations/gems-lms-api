package com.gems.education.application;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.response.CourseListResponse;
import com.gems.education.application.response.CourseResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GetAllCoursesUseCase {
  private final CourseGateway courseGateway;

  public GetAllCoursesUseCase(CourseGateway courseGateway) {
    this.courseGateway = courseGateway;
  }

  /** Legacy unpaginated listing, kept for callers that need every course at once. */
  public Flux<CourseResponse> execute() {
    return courseGateway.findAll()
      .map(CourseResponseMapper::toResponse);
  }

  public Mono<CourseListResponse> execute(String search, String status, String difficulty, int page, int limit) {
    int safePage = Math.max(page, 1);
    int safeLimit = Math.max(limit, 1);
    int offset = (safePage - 1) * safeLimit;

    Flux<CourseResponse> courses = courseGateway.findPage(search, status, difficulty, offset, safeLimit)
      .map(CourseResponseMapper::toResponse);

    return courses.collectList()
      .zipWith(courseGateway.count(search, status, difficulty))
      .map(tuple -> {
        var list = tuple.getT1();
        long total = tuple.getT2();
        int totalPages = (int) Math.ceil((double) total / safeLimit);
        return new CourseListResponse(
          list, total, safePage, safeLimit, totalPages,
          safePage < totalPages, safePage > 1
        );
      });
  }
}
