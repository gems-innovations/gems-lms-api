package com.gems.education.domain.entities;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A cohort: students of one institution taught together by an instructor and enrolled as a
 * block in courses and learning paths ("curso 1 grupo 1" vs "curso 1 grupo 2").
 * Student and instructor ids are ms-auth user ids.
 */
public record Group(
  Long id,
  String name,
  String institutionId,
  Long instructorId,
  List<Long> studentIds,
  List<Long> courseIds,
  List<Long> pathIds,
  LocalDateTime createdAt
) {
  public Group {
    studentIds = studentIds == null ? List.of() : List.copyOf(studentIds);
    courseIds = courseIds == null ? List.of() : List.copyOf(courseIds);
    pathIds = pathIds == null ? List.of() : List.copyOf(pathIds);
  }
}
