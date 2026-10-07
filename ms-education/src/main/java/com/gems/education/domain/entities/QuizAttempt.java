package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/**
 * A student's graded attempt at a quiz content block. Answers and per-question feedback
 * are kept as JSON documents shaped by the web client.
 */
public record QuizAttempt(
  Long id,
  Long enrollmentId,
  Long studentId,
  Long courseId,
  Long blockId,
  Long lessonId,
  int attemptNumber,
  String answers,
  int score,
  boolean passed,
  String feedback,
  LocalDateTime completedAt
) {
}
