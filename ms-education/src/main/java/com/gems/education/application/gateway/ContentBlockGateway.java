package com.gems.education.application.gateway;

import reactor.core.publisher.Mono;

/**
 * Reads content blocks of a course. Blocks are stored as JSON written by the web client, so
 * grading (which needs the questions and correct answers) lives behind this port.
 */
public interface ContentBlockGateway {

  /** Where the block lives and what it is; empty when the course has no such block. */
  Mono<BlockInfo> find(Long courseId, Long blockId);

  /** Grades the answers (JSON array of {questionId, answer}) against a quiz block. */
  Mono<Grading> grade(Long courseId, Long blockId, String answers);

  record BlockInfo(Long lessonId, String type, int maxAttempts) {
  }

  /** score is a percentage (0-100); feedback is a JSON array of {questionId, correct, explanation}. */
  record Grading(int score, boolean passed, String feedback) {
  }
}
