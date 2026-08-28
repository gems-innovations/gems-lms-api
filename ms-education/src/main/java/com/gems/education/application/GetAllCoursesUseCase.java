package com.gems.education.application;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.application.response.ContentResponse;
import com.gems.education.application.response.LessonResponse;
import com.gems.education.application.response.ModuleResponse;
import com.gems.education.domain.entities.Course;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GetAllCoursesUseCase {
  private final CourseGateway courseGateway;

  public GetAllCoursesUseCase(CourseGateway courseGateway) {
    this.courseGateway = courseGateway;
  }

  public Flux<CourseResponse> execute() {
    return courseGateway.findAll()
      .map(this::mapToResponse);
  }

  private CourseResponse mapToResponse(Course course) {
    List<ModuleResponse> moduleResponses = new ArrayList<>();
    if (course.getModules() != null) {
      moduleResponses = course.getModules().stream().map(module -> {
        List<LessonResponse> lessonResponses = new ArrayList<>();
        if (module.getLessons() != null) {
          lessonResponses = module.getLessons().stream().map(lesson -> {
            List<ContentResponse> contentResponses = new ArrayList<>();
            if (lesson.getContents() != null) {
              contentResponses = lesson.getContents().stream()
                .map(content -> new ContentResponse(content.getId(), content.getLessonId(), content.getType(), content.getValue(), content.getOrderIndex()))
                .collect(Collectors.toList());
            }
            return new LessonResponse(lesson.getId(), lesson.getModuleId(), lesson.getTitle(), lesson.getOrderIndex(), contentResponses);
          }).collect(Collectors.toList());
        }
        return new ModuleResponse(module.getId(), module.getCourseId(), module.getTitle(), module.getOrderIndex(), lessonResponses);
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
