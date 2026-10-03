package com.gems.education.application;

import com.gems.education.application.command.CourseCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.domain.entities.Content;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Lesson;
import com.gems.education.domain.entities.Module;
import com.gems.education.application.exceptions.CourseNotFoundException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UpdateCourseUseCase {
  private final CourseGateway courseGateway;

  public UpdateCourseUseCase(CourseGateway courseGateway) {
    this.courseGateway = courseGateway;
  }

  public Mono<CourseResponse> execute(Long id, CourseCommand command) {
    return courseGateway.findById(id)
      .switchIfEmpty(Mono.error(new CourseNotFoundException("Course not found with ID " + id)))
      .flatMap(existing -> {
        if (command.getTitle() != null) existing.setTitle(command.getTitle());
        if (command.getDescription() != null) existing.setDescription(command.getDescription());
        if (command.getDifficulty() != null) existing.setDifficulty(command.getDifficulty());
        if (command.getTags() != null) existing.setTags(command.getTags());
        if (command.getThumbnailUrl() != null) existing.setThumbnailUrl(command.getThumbnailUrl());
        if (command.getInstructorName() != null) existing.setInstructorName(command.getInstructorName());

        boolean isBeingPublished = command.getStatus() != null
          && "published".equals(command.getStatus())
          && !"published".equals(existing.getStatus());
        if (command.getStatus() != null) existing.setStatus(command.getStatus());
        if (isBeingPublished) existing.setPublishedAt(LocalDateTime.now());

        existing.setUpdatedAt(LocalDateTime.now());

        if (command.getModules() != null) {
          List<Module> modules = new ArrayList<>();
          command.getModules().forEach(mCmd -> {
            List<Lesson> lessons = new ArrayList<>();
            if (mCmd.getLessons() != null) {
              mCmd.getLessons().forEach(lCmd -> {
                List<Content> contents = new ArrayList<>();
                if (lCmd.getContents() != null) {
                  lCmd.getContents().forEach(cCmd ->
                    contents.add(new Content(cCmd.getId(), null, cCmd.getType(), cCmd.getValue(), cCmd.getOrderIndex())));
                }
                lessons.add(new Lesson(lCmd.getId(), null, lCmd.getTitle(), lCmd.getOrderIndex(), contents)
                  .details(lCmd.getDescription(), lCmd.getIsFree()));
              });
            }
            modules.add(new Module(mCmd.getId(), null, mCmd.getTitle(), mCmd.getOrderIndex(), lessons)
              .details(mCmd.getDescription()));
          });
          existing.setModules(modules);
          existing.setTotalLessons(modules.stream().mapToInt(m -> m.getLessons() == null ? 0 : m.getLessons().size()).sum());
        }

        return courseGateway.save(existing)
          .map(CourseResponseMapper::toResponse);
      });
  }
}
