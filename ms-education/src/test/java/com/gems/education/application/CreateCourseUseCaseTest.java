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
class CreateCourseUseCaseTest {

  @Mock
  private CourseGateway courseGateway;

  @InjectMocks
  private CreateCourseUseCase createCourseUseCase;

  private CourseCommand courseCommand;
  private Course savedCourse;

  @BeforeEach
  void setUp() {
    List<ContentCommand> contentCommands = List.of(new ContentCommand("VIDEO", "http://video.url", 1));
    List<LessonCommand> lessonCommands = List.of(new LessonCommand("Lesson 1", 1, contentCommands));
    List<ModuleCommand> moduleCommands = List.of(new ModuleCommand("Module 1", 1, lessonCommands));

    courseCommand = new CourseCommand(
      "Java Course",
      "Java fundamentals",
      "DRAFT",
      "inst-1",
      moduleCommands
    );

    List<Content> contents = List.of(new Content(1L, 1L, "VIDEO", "http://video.url", 1));
    List<Lesson> lessons = List.of(new Lesson(1L, 1L, "Lesson 1", 1, contents));
    List<Module> modules = List.of(new Module(1L, 1L, "Module 1", 1, lessons));

    savedCourse = new Course(
      1L,
      "Java Course",
      "Java fundamentals",
      "DRAFT",
      "inst-1",
      LocalDateTime.now(),
      LocalDateTime.now(),
      modules
    );
  }

  @Test
  void shouldCreateCourseSuccessfully() {
    // Given
    when(courseGateway.save(any(Course.class))).thenReturn(Mono.just(savedCourse));

    // When
    Mono<CourseResponse> result = createCourseUseCase.execute(courseCommand);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.getId().equals(1L) &&
          response.getTitle().equals("Java Course") &&
          response.getModules().size() == 1 &&
          response.getModules().get(0).getLessons().get(0).getContents().get(0).getType().equals("VIDEO")
      )
      .verifyComplete();

    verify(courseGateway, times(1)).save(any(Course.class));
  }
}
