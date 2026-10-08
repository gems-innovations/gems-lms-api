package com.gems.education.application;

import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.domain.entities.LearningPath;
import com.gems.education.application.exceptions.LearningPathNotFoundException;
import reactor.core.publisher.Mono;


public class GetLearningPathByIdUseCase {
  private final LearningPathGateway learningPathGateway;

  public GetLearningPathByIdUseCase(LearningPathGateway learningPathGateway) {
    this.learningPathGateway = learningPathGateway;
  }

  public Mono<LearningPathResponse> execute(Long id) {
    return learningPathGateway.findById(id)
      .map(this::mapToResponse)
      .switchIfEmpty(Mono.error(new LearningPathNotFoundException("Learning path not found with ID " + id)));
  }

  private LearningPathResponse mapToResponse(LearningPath lp) {
    return LearningPathResponses.toResponse(lp);
  }
}
