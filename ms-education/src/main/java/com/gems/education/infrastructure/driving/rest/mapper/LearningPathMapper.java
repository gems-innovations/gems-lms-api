package com.gems.education.infrastructure.driving.rest.mapper;

import com.gems.education.application.command.LearningPathCommand;
import com.gems.education.domain.entities.LearningPathStep;
import com.gems.education.infrastructure.driving.rest.request.LearningPathRequest;

import java.util.ArrayList;
import java.util.List;

public class LearningPathMapper {
  private LearningPathMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static LearningPathCommand toCommand(LearningPathRequest request) {
    List<Long> courseIds = request.getCourseIds() != null ? request.getCourseIds() : new ArrayList<>();
    List<LearningPathStep> steps = null;
    if (request.getSteps() != null) {
      steps = request.getSteps().stream()
        .filter(s -> s.courseId() != null)
        .map(s -> new LearningPathStep(s.courseId(), !Boolean.FALSE.equals(s.required()), s.minimumScore()))
        .toList();
      courseIds = steps.stream().map(LearningPathStep::courseId).toList();
    }
    LearningPathCommand command = new LearningPathCommand(
      request.getTitle(),
      request.getDescription(),
      request.getInstitutionId(),
      courseIds
    );
    command.setStatus(request.getStatus());
    command.setTags(request.getTags());
    command.setThumbnailUrl(request.getThumbnailUrl());
    command.setSteps(steps);
    return command;
  }
}
