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
import com.gems.education.infrastructure.driving.rest.exeption.CourseNotFoundException;
import com.gems.education.infrastructure.driving.rest.exeption.LearningPathNotFoundException;
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

        if (command.getCourseIds() == null || command.getCourseIds().isEmpty()) {
          existing.setCourses(new ArrayList<>());
          return learningPathGateway.save(existing);
        }

        return Flux.fromIterable(command.getCourseIds())
          .flatMap(courseId -> courseGateway.findById(courseId)
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
