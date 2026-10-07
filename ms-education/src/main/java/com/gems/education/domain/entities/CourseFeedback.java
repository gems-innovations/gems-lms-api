package com.gems.education.domain.entities;

import java.time.LocalDateTime;

/** What students tell about a course: the course survey, its responses and course reviews. */
public final class CourseFeedback {
  private CourseFeedback() {
  }

  /** The instructor's feedback survey of a course; {@code sections} is a JSON array shaped by the web client. */
  public record Survey(Long id, Long courseId, String title, String description, String sections,
                       boolean published, LocalDateTime updatedAt) {
  }

  /** One student's answers to a course survey; {@code answers} is a JSON array of {questionId, value}. */
  public record SurveyResponse(Long id, Long surveyId, Long courseId, Long studentId, String answers,
                               LocalDateTime submittedAt) {
  }

  /** A student's rating (1–5) and comment of a course; one per student and course. */
  public record Review(Long id, Long courseId, Long studentId, int rating, String comment,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
  }
}
