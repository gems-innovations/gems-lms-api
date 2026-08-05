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
    List<CourseResponse> courseResponses = new ArrayList<>();
    if (lp.getCourses() != null) {
      courseResponses = lp.getCourses().stream().map(this::mapCourseToResponse).collect(Collectors.toList());
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

  private CourseResponse mapCourseToResponse(Course course) {
    List<ModuleResponse> moduleResponses = new ArrayList<>();
    if (course.getModules() != null) {
      moduleResponses = course.getModules().stream().map(m -> {
        List<LessonResponse> lessonResponses = new ArrayList<>();
        if (m.getLessons() != null) {
          lessonResponses = m.getLessons().stream().map(l -> {
            List<ContentResponse> contentResponses = new ArrayList<>();
            if (l.getContents() != null) {
              contentResponses = l.getContents().stream()
                .map(c -> new ContentResponse(c.getId(), c.getLessonId(), c.getType(), c.getValue(), c.getOrderIndex()))
                .collect(Collectors.toList());
            }
            return new LessonResponse(l.getId(), l.getModuleId(), l.getTitle(), l.getOrderIndex(), contentResponses);
          }).collect(Collectors.toList());
        }
        return new ModuleResponse(m.getId(), m.getCourseId(), m.getTitle(), m.getOrderIndex(), lessonResponses);
      }).collect(Collectors.toList());
    }

    return new CourseResponse(
      course.getId(),
      course.getTitle(),
      course.getDescription(),
      course.getStatus(),
      course.getInstitutionId(),
      course.getCreatedAt(),
      course.getUpdatedAt(),
      moduleResponses
    );
  }
}
