package com.gems.education.application;

import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.domain.entities.LearningPath;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    List<CourseResponse> courseResponses = new ArrayList<>();
    if (learningPath.getCourses() != null) {
      courseResponses = learningPath.getCourses().stream()
        .map(c -> new CourseResponse(
          c.getId(), c.getTitle(), c.getDescription(), c.getStatus(),
          c.getInstitutionId(), c.getCreatedAt(), c.getUpdatedAt(), new ArrayList<>()
        ))
        .collect(Collectors.toList());
    }

    return new LearningPathResponse(
      learningPath.getId(),
      learningPath.getTitle(),
      learningPath.getDescription(),
      learningPath.getInstitutionId(),
      learningPath.getCreatedAt(),
      courseResponses
    );
  }
}
