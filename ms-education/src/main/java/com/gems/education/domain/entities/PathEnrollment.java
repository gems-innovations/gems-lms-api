package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/** A student's enrollment in a learning path (the courses of the path are enrolled separately). */
public record PathEnrollment(
  Long id,
  Long learningPathId,
  Long studentId,
  String status,
  LocalDateTime enrolledAt,
  LocalDateTime completedAt
) {
  public static final String ACTIVE = "active";
  public static final String COMPLETED = "completed";
}
