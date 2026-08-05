package com.gems.education.infrastructure.driving.rest.mapper;

import com.gems.education.application.command.LearningPathCommand;
import com.gems.education.infrastructure.driving.rest.request.LearningPathRequest;

import java.util.ArrayList;

public class LearningPathMapper {
  private LearningPathMapper() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static LearningPathCommand toCommand(LearningPathRequest request) {
    return new LearningPathCommand(
      request.getTitle(),
      request.getDescription(),
      request.getInstitutionId(),
      request.getCourseIds() != null ? request.getCourseIds() : new ArrayList<>()
    );
  }
}
