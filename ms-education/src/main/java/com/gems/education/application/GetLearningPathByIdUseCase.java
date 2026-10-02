package com.gems.education.application;

import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.response.ContentResponse;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.application.response.LessonResponse;
import com.gems.education.application.response.ModuleResponse;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.LearningPath;
import com.gems.education.infrastructure.driving.rest.exeption.LearningPathNotFoundException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
