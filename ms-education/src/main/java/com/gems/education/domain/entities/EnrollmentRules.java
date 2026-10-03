package com.gems.education.domain.entities;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Who may enroll in a course and when. Null bounds and capacity mean "no limit"; selfEnrollment
 * false means only staff enroll students.
 */
public record EnrollmentRules(
  Long courseId,
  Long periodId,
  LocalDateTime opensAt,
  LocalDateTime closesAt,
  Integer capacity,
  boolean selfEnrollment,
  List<Long> prerequisiteIds
) {
  public static EnrollmentRules none(Long courseId) {
    return new EnrollmentRules(courseId, null, null, null, null, true, List.of());
  }
}
