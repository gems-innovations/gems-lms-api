package com.gems.education.application;

import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.response.ContentResponse;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.application.response.LessonResponse;
import com.gems.education.application.response.ModuleResponse;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.LearningPath;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    List<CourseResponse> courseResponses = new ArrayList<>();
    if (lp.getCourses() != null) {
      courseResponses = lp.getCourses().stream().map(CourseResponseMapper::toResponse).collect(Collectors.toList());
    }

    return new LearningPathResponse(
      lp.getId(),
      lp.getTitle(),
      lp.getDescription(),
      lp.getInstitutionId(),
      lp.getCreatedAt(),
      courseResponses
    );
  }
}
