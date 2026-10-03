package com.gems.education.application;

import com.gems.education.application.command.LearningPathCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.response.ContentResponse;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.application.response.LearningPathResponse;
import com.gems.education.application.response.LessonResponse;
import com.gems.education.application.response.ModuleResponse;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.LearningPath;
import com.gems.education.application.exceptions.CourseNotFoundException;
import com.gems.education.application.exceptions.LearningPathNotFoundException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateLearningPathUseCase {
  private final LearningPathGateway learningPathGateway;
  private final CourseGateway courseGateway;

  public UpdateLearningPathUseCase(LearningPathGateway learningPathGateway, CourseGateway courseGateway) {
    this.learningPathGateway = learningPathGateway;
    this.courseGateway = courseGateway;
  }

  public Mono<LearningPathResponse> execute(Long id, LearningPathCommand command) {
    return learningPathGateway.findById(id)
      .switchIfEmpty(Mono.error(new LearningPathNotFoundException("Learning path not found with ID " + id)))
      .flatMap(existing -> {
        existing.setTitle(command.getTitle());
        existing.setDescription(command.getDescription());
        if (command.getStatus() != null) existing.setStatus(command.getStatus());
        if (command.getTags() != null) existing.setTags(command.getTags());
        if (command.getThumbnailUrl() != null) existing.setThumbnailUrl(command.getThumbnailUrl());
        if (command.getSteps() != null) existing.setSteps(command.getSteps());
        return Flux.fromIterable(command.getCourseIds() == null ? List.<Long>of() : command.getCourseIds())
          .concatMap(courseId -> courseGateway.findById(courseId)
            .switchIfEmpty(Mono.error(new CourseNotFoundException("Course not found with ID " + courseId))))
          .collectList()
          .flatMap(courses -> {
            existing.setCourses(courses);
            return learningPathGateway.save(existing);
          });
      })
      .map(this::mapToResponse);
  }

  private LearningPathResponse mapToResponse(LearningPath lp) {
    return LearningPathResponses.toResponse(lp);
  }
}
