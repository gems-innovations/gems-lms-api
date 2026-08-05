package com.gems.education.application;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.response.ContentResponse;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.application.response.LessonResponse;
import com.gems.education.application.response.ModuleResponse;
import com.gems.education.domain.entities.Course;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GetCoursesByInstitutionUseCase {
  private final CourseGateway courseGateway;

  public GetCoursesByInstitutionUseCase(CourseGateway courseGateway) {
    this.courseGateway = courseGateway;
  }

  public Flux<CourseResponse> execute(String institutionId) {
    return courseGateway.findByInstitutionId(institutionId)
      .map(this::mapToResponse);
  }

  private CourseResponse mapToResponse(Course course) {
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
