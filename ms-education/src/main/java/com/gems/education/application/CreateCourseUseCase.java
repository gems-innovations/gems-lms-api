package com.gems.education.application;

import com.gems.education.application.command.CourseCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.domain.entities.Content;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Lesson;
import com.gems.education.domain.entities.Module;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CreateCourseUseCase {
  private final CourseGateway courseGateway;

  public CreateCourseUseCase(CourseGateway courseGateway) {
    this.courseGateway = courseGateway;
  }

  public Mono<CourseResponse> execute(CourseCommand command) {
    if (command.getTitle() == null || command.getTitle().isBlank()) {
      return Mono.error(new IllegalArgumentException("Course title is required"));
    }
    if (command.getInstitutionId() == null || command.getInstitutionId().isBlank()) {
      return Mono.error(new IllegalArgumentException("Institution ID is required"));
    }

    Course course = new Course();
    course.setTitle(command.getTitle());
    course.setDescription(command.getDescription());
    String status = command.getStatus() != null && !command.getStatus().isBlank() ? command.getStatus() : "draft";
    course.setStatus(status);
    course.setDifficulty(command.getDifficulty() != null && !command.getDifficulty().isBlank() ? command.getDifficulty() : "beginner");
    course.setTags(command.getTags() != null ? command.getTags() : new ArrayList<>());
    course.setThumbnailUrl(command.getThumbnailUrl());
    course.setInstructorName(command.getInstructorName());
    course.setInstitutionId(command.getInstitutionId());
    course.setEnrolledCount(0);
    course.setCompletionRate(0);
    course.setRatingCount(0);
    course.setTotalDuration(0);
    course.setCreatedAt(LocalDateTime.now());
    course.setUpdatedAt(LocalDateTime.now());
    course.setPublishedAt("published".equals(status) ? LocalDateTime.now() : null);

    List<Module> modules = new ArrayList<>();
    if (command.getModules() != null) {
      command.getModules().forEach(mCmd -> {
        List<Lesson> lessons = new ArrayList<>();
        if (mCmd.getLessons() != null) {
          mCmd.getLessons().forEach(lCmd -> {
            List<Content> contents = new ArrayList<>();
            if (lCmd.getContents() != null) {
              lCmd.getContents().forEach(cCmd ->
                contents.add(new Content(null, null, cCmd.getType(), cCmd.getValue(), cCmd.getOrderIndex())));
            }
            lessons.add(new Lesson(null, null, lCmd.getTitle(), lCmd.getOrderIndex(), contents)
              .details(lCmd.getDescription(), lCmd.getIsFree()));
          });
        }
        modules.add(new Module(null, null, mCmd.getTitle(), mCmd.getOrderIndex(), lessons).details(mCmd.getDescription()));
      });
    }
    course.setModules(modules);
    course.setTotalLessons(modules.stream().mapToInt(m -> m.getLessons() == null ? 0 : m.getLessons().size()).sum());

    return courseGateway.save(course)
      .map(CourseResponseMapper::toResponse);
  }
}
