package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/** A student's delivery for an assignment content block; one per enrollment and block. */
public record AssignmentSubmission(
  Long id,
  Long enrollmentId,
  Long studentId,
  Long courseId,
  Long blockId,
  Long lessonId,
  String textContent,
  String fileUrls,
  LocalDateTime submittedAt,
  Integer grade,
  String feedback,
  String status
) {
  public static final String PENDING = "pending";
  public static final String GRADED = "graded";

  public AssignmentSubmission graded(int newGrade, String newFeedback) {
    return new AssignmentSubmission(id, enrollmentId, studentId, courseId, blockId, lessonId, textContent,
      fileUrls, submittedAt, newGrade, newFeedback, GRADED);
  }
}
