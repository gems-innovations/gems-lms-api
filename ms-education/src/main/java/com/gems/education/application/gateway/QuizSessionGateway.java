package com.gems.education.application.gateway;

import com.gems.education.domain.entities.QuizSession;
import reactor.core.publisher.Mono;

public interface QuizSessionGateway {
  Mono<QuizSession> save(QuizSession session);

  Mono<QuizSession> findById(Long id);

  /** The unsubmitted session of the enrollment for the block, if any. */
  Mono<QuizSession> findOpen(Long enrollmentId, Long blockId);

  /** Closes the session; empty when it was already closed (a concurrent submit won). */
  Mono<QuizSession> close(Long id);
}
