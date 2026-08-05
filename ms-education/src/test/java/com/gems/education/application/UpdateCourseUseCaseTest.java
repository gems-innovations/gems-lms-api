package com.gems.education.application;

import com.gems.education.application.command.ContentCommand;
import com.gems.education.application.command.CourseCommand;
import com.gems.education.application.command.LessonCommand;
import com.gems.education.application.command.ModuleCommand;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.domain.entities.Content;
import com.gems.education.domain.entities.Course;
import com.gems.education.domain.entities.Lesson;
import com.gems.education.domain.entities.Module;
import com.gems.education.infrastructure.driving.rest.exeption.CourseNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCourseUseCaseTest {

  @Mock
  private CourseGateway courseGateway;

  @InjectMocks
  private UpdateCourseUseCase updateCourseUseCase;

  private CourseCommand courseCommand;
  private Course existingCourse;
  private Course updatedCourse;

  @BeforeEach
  void setUp() {
    List<ContentCommand> contentCommands = List.of(new ContentCommand("TEXT", "Updated Content", 1));
    List<LessonCommand> lessonCommands = List.of(new LessonCommand("Updated Lesson 1", 1, contentCommands));
    List<ModuleCommand> moduleCommands = List.of(new ModuleCommand("Updated Module 1", 1, lessonCommands));

    courseCommand = new CourseCommand(
      "Java Course Updated",
      "Java fundamentals updated",
      "PUBLISHED",
      "inst-1",
      moduleCommands
    );

    existingCourse = new Course(
      1L,
      "Java Course",
      "Java fundamentals",
      "DRAFT",
      "inst-1",
      LocalDateTime.now(),
      LocalDateTime.now(),
      List.of()
    );

    List<Content> updatedContents = List.of(new Content(1L, 1L, "TEXT", "Updated Content", 1));
    List<Lesson> updatedLessons = List.of(new Lesson(1L, 1L, "Updated Lesson 1", 1, updatedContents));
    List<Module> updatedModules = List.of(new Module(1L, 1L, "Updated Module 1", 1, updatedLessons));

    updatedCourse = new Course(
      1L,
      "Java Course Updated",
      "Java fundamentals updated",
      "PUBLISHED",
      "inst-1",
      existingCourse.getCreatedAt(),
      LocalDateTime.now(),
      updatedModules
    );
  }

  @Test
  void shouldUpdateCourseSuccessfully() {
    // Given
    when(courseGateway.findById(1L)).thenReturn(Mono.just(existingCourse));
    when(courseGateway.save(any(Course.class))).thenReturn(Mono.just(updatedCourse));

    // When
    Mono<CourseResponse> result = updateCourseUseCase.execute(1L, courseCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.getId().equals(1L) &&
          response.getTitle().equals("Java Course Updated") &&
          response.getDescription().equals("Java fundamentals updated") &&
          response.getStatus().equals("PUBLISHED") &&
          response.getModules().size() == 1 &&
          response.getModules().get(0).getLessons().get(0).getContents().get(0).getType().equals("TEXT")
      )
      .verifyComplete();

    verify(courseGateway, times(1)).findById(1L);
    verify(courseGateway, times(1)).save(any(Course.class));
  }

  @Test
  void shouldThrowNotFoundWhenUpdatingNonExistentCourse() {
    // Given
    when(courseGateway.findById(2L)).thenReturn(Mono.empty());

    // When
    Mono<CourseResponse> result = updateCourseUseCase.execute(2L, courseCommand);

    // Then
    StepVerifier.create(result)
      .expectError(CourseNotFoundException.class)
      .verify();

    verify(courseGateway, times(1)).findById(2L);
    verify(courseGateway, never()).save(any(Course.class));
  }
}
