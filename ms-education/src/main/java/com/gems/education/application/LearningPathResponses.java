package com.gems.education.application;

import com.gems.education.application.response.CourseResponse;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.LearningPath;
import com.gems.education.domain.entities.LearningPathStep;

import java.util.List;

/** Maps a learning path, with its courses and per-course settings, to its API response. */
final class LearningPathResponses {
  private LearningPathResponses() {
  }

  static LearningPathResponse toResponse(LearningPath lp) {
    List<Course> courses = lp.getCourses() == null ? List.of() : lp.getCourses();
    List<CourseResponse> courseResponses = courses.stream().map(CourseResponseMapper::toResponse).toList();
    LearningPathResponse response = new LearningPathResponse(lp.getId(), lp.getTitle(), lp.getDescription(),
      lp.getInstitutionId(), lp.getCreatedAt(), courseResponses);
    response.setStatus(lp.getStatus());
    response.setTags(lp.getTags());
    response.setThumbnailUrl(lp.getThumbnailUrl());
    response.setUpdatedAt(lp.getUpdatedAt() != null ? lp.getUpdatedAt() : lp.getCreatedAt());
    response.setSteps(courses.stream().map(c -> {
      LearningPathStep step = lp.stepFor(c.getId());
      return new LearningPathResponse.StepResponse(c.getId(), step.required(), step.minimumScore());
    }).toList());
    response.setEnrolledCount(lp.getEnrolledCount());
    response.setCompletionRate(lp.getCompletionRate());
    return response;
  }
}
