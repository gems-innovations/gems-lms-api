package com.gems.education.application;

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
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCoursesByInstitutionUseCaseTest {

  @Mock
  private CourseGateway courseGateway;

  @InjectMocks
  private GetCoursesByInstitutionUseCase getCoursesByInstitutionUseCase;

  private Course course1;
  private Course course2;

  @BeforeEach
  void setUp() {
    List<Content> contents = List.of(new Content(1L, 1L, "VIDEO", "http://video.url", 1));
    List<Lesson> lessons = List.of(new Lesson(1L, 1L, "Lesson 1", 1, contents));
    List<Module> modules = List.of(new Module(1L, 1L, "Module 1", 1, lessons));

    course1 = new Course(
      1L,
      "Java Course 1",
      "Java fundamentals",
      "PUBLISHED",
      "inst-1",
      LocalDateTime.now(),
      LocalDateTime.now(),
      modules
    );

    course2 = new Course(
      2L,
      "Java Course 2",
      "Advanced Java",
      "DRAFT",
      "inst-1",
      LocalDateTime.now(),
      LocalDateTime.now(),
      List.of()
    );
  }

  @Test
  void shouldReturnCoursesForInstitution() {
    // Given
    when(courseGateway.findByInstitutionId("inst-1")).thenReturn(Flux.just(course1, course2));

    // When
    Flux<CourseResponse> result = getCoursesByInstitutionUseCase.execute("inst-1");

    // Then
    StepVerifier.create(result)
      .expectNextMatches(response ->
        response.getId().equals(1L) &&
          response.getTitle().equals("Java Course 1") &&
          response.getModules().size() == 1
      )
      .expectNextMatches(response ->
        response.getId().equals(2L) &&
          response.getTitle().equals("Java Course 2") &&
          response.getModules().isEmpty()
      )
      .verifyComplete();

    verify(courseGateway, times(1)).findByInstitutionId("inst-1");
  }

  @Test
  void shouldReturnEmptyFluxWhenNoCoursesFound() {
    // Given
    when(courseGateway.findByInstitutionId("inst-empty")).thenReturn(Flux.empty());

    // When
    Flux<CourseResponse> result = getCoursesByInstitutionUseCase.execute("inst-empty");

    // Then
    StepVerifier.create(result)
      .verifyComplete();

    verify(courseGateway, times(1)).findByInstitutionId("inst-empty");
  }
}
