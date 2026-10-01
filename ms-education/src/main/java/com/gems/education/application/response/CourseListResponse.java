package com.gems.education.application.response;

import java.util.List;

public record CourseListResponse(
  List<CourseResponse> courses,
  long total,
  int page,
  int limit,
  int totalPages,
  boolean hasNext,
  boolean hasPrevious
) {
}
