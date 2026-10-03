package com.gems.education.application.gateway;

import reactor.core.publisher.Mono;

/**
 * Builds the questions of one quiz attempt from a quiz block (its own questions plus random draws
 * from the question bank, shuffled when the block asks for it) and grades answers against them.
 * Lives behind a port because quiz blocks are JSON written by the web client.
 */
public interface QuizComposer {

  Mono<ComposedQuiz> compose(Long courseId, Long blockId);

  /** Grades answers (JSON [{questionId, answer}]) against the composed questions. */
  ContentBlockGateway.Grading grade(String questions, int passingScore, String answers);

  /**
   * questions: with answer keys; studentQuestions: without them. timeLimitSeconds is 0 when the
   * quiz is not timed.
   */
  record ComposedQuiz(String questions, String studentQuestions, int passingScore, int timeLimitSeconds) {
  }
}
