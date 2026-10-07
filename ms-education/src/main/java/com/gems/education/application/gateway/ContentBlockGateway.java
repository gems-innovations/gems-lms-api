package com.gems.education.application.gateway;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reads content blocks of a course. Blocks are stored as JSON written by the web client, so
 * grading (which needs the questions and correct answers) lives behind this port.
 */
public interface ContentBlockGateway {

  /** Where the block lives and what it is; empty when the course has no such block. */
  Mono<BlockInfo> find(Long courseId, Long blockId);

  /** Grades the answers (JSON array of {questionId, answer}) against a quiz block. */
  Mono<Grading> grade(Long courseId, Long blockId, String answers);

  /** Quiz and assignment blocks of the course, in course order. */
  Flux<GradableItem> gradableItems(Long courseId);

  /** Rubric criteria of an assignment block; empty list when it has none. */
  Mono<List<RubricCriterion>> rubric(Long courseId, Long blockId);

  record GradableItem(Long blockId, Long lessonId, String type, String title) {
  }

  record RubricCriterion(String id, String criterion, int maxPoints) {
  }

  /** sessionRequired: the quiz draws from the bank, is timed or shuffled, so attempts start with a session. */
  record BlockInfo(Long lessonId, String type, int maxAttempts, boolean sessionRequired) {
  }

  /** score is a percentage (0-100); feedback is a JSON array of {questionId, correct, explanation}. */
  record Grading(int score, boolean passed, String feedback) {
  }
}
