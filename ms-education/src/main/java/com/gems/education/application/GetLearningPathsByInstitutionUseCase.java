package com.gems.education.application;

import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.domain.entities.LearningPath;
import reactor.core.publisher.Flux;


public class GetLearningPathsByInstitutionUseCase {
  private final LearningPathGateway learningPathGateway;

  public GetLearningPathsByInstitutionUseCase(LearningPathGateway learningPathGateway) {
    this.learningPathGateway = learningPathGateway;
  }

  public Flux<LearningPathResponse> execute(String institutionId) {
    return learningPathGateway.findByInstitutionId(institutionId)
      .map(this::mapToResponse);
  }

  private LearningPathResponse mapToResponse(LearningPath lp) {
    return LearningPathResponses.toResponse(lp);
  }
}
