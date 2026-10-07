package com.gems.education.application.gateway;

import reactor.core.publisher.Mono;

import java.util.Map;

/** Weight of each gradable block in a course grade. Blocks without a stored weight count 1. */
public interface GradebookGateway {
  Mono<Map<Long, Integer>> weights(Long courseId);

  /** Replaces all the weights of the course. */
  Mono<Void> saveWeights(Long courseId, Map<Long, Integer> weights);
}
