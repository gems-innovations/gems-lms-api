package com.gems.education.application;

import com.gems.education.application.command.CourseCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.response.ContentResponse;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.application.response.LessonResponse;
import com.gems.education.application.response.ModuleResponse;
import com.gems.education.domain.entities.Content;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Lesson;
import com.gems.education.domain.entities.Module;
import com.gems.education.infrastructure.driving.rest.exeption.CourseNotFoundException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateCourseUseCase {
  private final CourseGateway courseGateway;

  public UpdateCourseUseCase(CourseGateway courseGateway) {
    this.courseGateway = courseGateway;
  }

  public Mono<CourseResponse> execute(Long id, CourseCommand command) {
    return courseGateway.findById(id)
      .switchIfEmpty(Mono.error(new CourseNotFoundException("Course not found with ID " + id)))
      .flatMap(existing -> {
        existing.setTitle(command.getTitle());
        existing.setDescription(command.getDescription());
        existing.setStatus(command.getStatus() != null ? command.getStatus() : existing.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());

        List<Module> modules = new ArrayList<>();
        if (command.getModules() != null) {
          command.getModules().forEach(mCmd -> {
            List<Lesson> lessons = new ArrayList<>();
            if (mCmd.getLessons() != null) {
              mCmd.getLessons().forEach(lCmd -> {
                List<Content> contents = new ArrayList<>();
                if (lCmd.getContents() != null) {
                  lCmd.getContents().forEach(cCmd -> {
                    contents.add(new Content(null, null, cCmd.getType(), cCmd.getValue(), cCmd.getOrderIndex()));
                  });
                }
                lessons.add(new Lesson(null, null, lCmd.getTitle(), lCmd.getOrderIndex(), contents));
              });
            }
            modules.add(new Module(null, null, mCmd.getTitle(), mCmd.getOrderIndex(), lessons));
          });
        }
        existing.setModules(modules);

        return courseGateway.save(existing)
          .map(this::mapToResponse);
      });
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
