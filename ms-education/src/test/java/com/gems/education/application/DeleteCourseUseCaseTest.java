package com.gems.education.application;

import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.domain.entities.Course;
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
class DeleteCourseUseCaseTest {

  @Mock
  private CourseGateway courseGateway;

  @InjectMocks
  private DeleteCourseUseCase deleteCourseUseCase;

  private Course course;

  @BeforeEach
  void setUp() {
    course = new Course(
      1L,
      "Java Course",
      "Java fundamentals",
      "DRAFT",
      "inst-1",
      LocalDateTime.now(),
      LocalDateTime.now(),
      List.of()
    );
  }

  @Test
  void shouldDeleteCourseSuccessfully() {
    // Given
    when(courseGateway.findById(1L)).thenReturn(Mono.just(course));
    when(courseGateway.deleteById(1L)).thenReturn(Mono.empty());

    // When
    Mono<Void> result = deleteCourseUseCase.execute(1L);

    // Then
    StepVerifier.create(result)
      .verifyComplete();

    verify(courseGateway, times(1)).findById(1L);
    verify(courseGateway, times(1)).deleteById(1L);
  }

  @Test
  void shouldThrowNotFoundWhenDeletingNonExistentCourse() {
    // Given
    when(courseGateway.findById(2L)).thenReturn(Mono.empty());

    // When
    Mono<Void> result = deleteCourseUseCase.execute(2L);

    // Then
    StepVerifier.create(result)
      .expectError(CourseNotFoundException.class)
      .verify();

    verify(courseGateway, times(1)).findById(2L);
    verify(courseGateway, never()).deleteById(anyLong());
  }
}
