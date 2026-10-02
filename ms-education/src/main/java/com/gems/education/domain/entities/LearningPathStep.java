package com.gems.education.domain.entities;

/**
 * Per-course settings of a learning path: whether the course must be completed to finish the
 * path, and the minimum score (percentage) required to advance past it, if any.
 */
public record LearningPathStep(Long courseId, boolean required, Integer minimumScore) {
  public static LearningPathStep requiredCourse(Long courseId) {
    return new LearningPathStep(courseId, true, null);
  }
}
