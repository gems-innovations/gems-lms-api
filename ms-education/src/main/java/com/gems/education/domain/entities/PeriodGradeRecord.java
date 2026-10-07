package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/**
 * A student's frozen result in a course when its academic period closed (one line of the acta).
 * finalGrade counts missing work as 0; passed compares it with the passing grade.
 */
public record PeriodGradeRecord(
  Long periodId,
  Long courseId,
  String courseTitle,
  Long studentId,
  Long enrollmentId,
  Double finalGrade,
  Double currentGrade,
  Integer progress,
  boolean passed,
  LocalDateTime recordedAt
) {
}
