package com.gems.education.application;

import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.domain.entities.LearningPath;
import reactor.core.publisher.Flux;


public class GetAllLearningPathsUseCase {
  private final LearningPathGateway learningPathGateway;

  public GetAllLearningPathsUseCase(LearningPathGateway learningPathGateway) {
    this.learningPathGateway = learningPathGateway;
  }

  public Flux<LearningPathResponse> execute() {
    return learningPathGateway.findAll()
      .map(this::mapToResponse);
  }

  private LearningPathResponse mapToResponse(LearningPath learningPath) {
    return LearningPathResponses.toResponse(learningPath);
  }
}
