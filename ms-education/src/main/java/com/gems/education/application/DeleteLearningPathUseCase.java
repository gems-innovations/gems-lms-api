package com.gems.education.application;

import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.infrastructure.driving.rest.exeption.LearningPathNotFoundException;
import reactor.core.publisher.Mono;

public class DeleteLearningPathUseCase {
  private final LearningPathGateway learningPathGateway;

  public DeleteLearningPathUseCase(LearningPathGateway learningPathGateway) {
    this.learningPathGateway = learningPathGateway;
  }

  public Mono<Void> execute(Long id) {
    return learningPathGateway.findById(id)
      .switchIfEmpty(Mono.error(new LearningPathNotFoundException("Learning path not found with ID " + id)))
      .flatMap(existing -> learningPathGateway.deleteById(id));
  }
}
