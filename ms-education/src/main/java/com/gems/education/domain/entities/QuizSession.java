package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/**
 * A quiz attempt in progress. questions holds the drawn and ordered questions with their answer
 * keys (graded against on submit); studentQuestions is the same list without them. expiresAt is
 * null when the quiz has no time limit.
 */
public record QuizSession(
  Long id,
  Long enrollmentId,
  Long studentId,
  Long courseId,
  Long blockId,
  String questions,
  String studentQuestions,
  int passingScore,
  LocalDateTime startedAt,
  LocalDateTime expiresAt,
  LocalDateTime submittedAt
) {
  public boolean expired(LocalDateTime now, long graceSeconds) {
    return expiresAt != null && now.isAfter(expiresAt.plusSeconds(graceSeconds));
  }
}
