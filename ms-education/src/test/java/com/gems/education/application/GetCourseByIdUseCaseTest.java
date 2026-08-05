package com.gems.education.application;

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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCourseByIdUseCaseTest {

  @Mock
  private CourseGateway courseGateway;

  @InjectMocks
  private GetCourseByIdUseCase getCourseByIdUseCase;

  private Course course;

  @BeforeEach
  void setUp() {
    List<Content> contents = List.of(new Content(1L, 1L, "VIDEO", "http://video.url", 1));
    List<Lesson> lessons = List.of(new Lesson(1L, 1L, "Lesson 1", 1, contents));
    List<Module> modules = List.of(new Module(1L, 1L, "Module 1", 1, lessons));

    course = new Course(
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
  void shouldReturnCourseSuccessfully() {
    // Given
    when(courseGateway.findById(1L)).thenReturn(Mono.just(course));

    // When
    Mono<CourseResponse> result = getCourseByIdUseCase.execute(1L);

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.getId().equals(1L) &&
          response.getTitle().equals("Java Course") &&
          response.getModules().size() == 1 &&
          response.getModules().get(0).getLessons().get(0).getContents().get(0).getType().equals("VIDEO")
      )
      .verifyComplete();

    verify(courseGateway, times(1)).findById(1L);
  }

  @Test
  void shouldThrowNotFoundWhenCourseDoesNotExist() {
    // Given
    when(courseGateway.findById(2L)).thenReturn(Mono.empty());

    // When
    Mono<CourseResponse> result = getCourseByIdUseCase.execute(2L);

    // Then
    StepVerifier.create(result)
      .expectError(CourseNotFoundException.class)
      .verify();

    verify(courseGateway, times(1)).findById(2L);
  }
}
