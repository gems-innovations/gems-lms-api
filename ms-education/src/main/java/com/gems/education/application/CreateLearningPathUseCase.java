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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreateLearningPathUseCase {
  private final LearningPathGateway learningPathGateway;
  private final CourseGateway courseGateway;

  public CreateLearningPathUseCase(LearningPathGateway learningPathGateway, CourseGateway courseGateway) {
    this.learningPathGateway = learningPathGateway;
    this.courseGateway = courseGateway;
  }

  public Mono<LearningPathResponse> execute(LearningPathCommand command) {
    LearningPath lp = new LearningPath(null, command.getTitle(), command.getDescription(), command.getInstitutionId(), LocalDateTime.now(), new ArrayList<>());
    lp.setStatus(command.getStatus() != null ? command.getStatus() : LearningPath.DRAFT);
    lp.setTags(command.getTags());
    lp.setThumbnailUrl(command.getThumbnailUrl());
    lp.setSteps(command.getSteps());
    return loadCourses(command.getCourseIds())
      .flatMap(courses -> {
        lp.setCourses(courses);
        return learningPathGateway.save(lp);
      })
      .map(this::mapToResponse);
  }

  /** The courses in the given order; fails if one does not exist. */
  private Mono<List<Course>> loadCourses(List<Long> courseIds) {
    return Flux.fromIterable(courseIds == null ? List.<Long>of() : courseIds)
      .concatMap(courseId -> courseGateway.findById(courseId)
        .switchIfEmpty(Mono.error(new CourseNotFoundException("Course not found with ID " + courseId))))
      .collectList();
  }

  private LearningPathResponse mapToResponse(LearningPath lp) {
    return LearningPathResponses.toResponse(lp);
  }
}
