package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.GradebookUseCase;
import com.gems.education.application.GradebookUseCase.Gradebook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class GradebookControllerTest extends ControllerTestSupport {
  private final GradebookUseCase gradebook = mock(GradebookUseCase.class);
  private GradebookController controller;

  @BeforeEach
  void setUp() {
    givenCourses();
    Gradebook empty = new Gradebook(1L, List.of(), List.of());
    when(gradebook.course(anyLong())).thenReturn(Mono.just(empty));
    when(gradebook.student(anyLong(), anyLong())).thenReturn(Mono.just(empty));
    when(gradebook.saveWeights(anyLong(), anyMap())).thenReturn(Mono.just(empty));
    com.gems.education.application.PeriodClosingUseCase closing = org.mockito.Mockito.mock(com.gems.education.application.PeriodClosingUseCase.class);
    when(closing.requireOpen(anyLong())).thenReturn(Mono.empty());
    controller = new GradebookController(gradebook, access, closing);
  }

  @Test
  void staffOfTheInstitutionSeeTheWholeGradebook() {
    client(controller, INSTRUCTOR).get().uri("/api/v1/courses/1/gradebook").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.courseId").isEqualTo(1);
    verify(gradebook).course(1L);
  }

  @Test
  void studentsAndOtherInstitutionsCannotSeeIt() {
    client(controller, STUDENT).get().uri("/api/v1/courses/1/gradebook").exchange().expectStatus().isForbidden();
    client(controller, OTHER_ADMIN).get().uri("/api/v1/courses/1/gradebook").exchange().expectStatus().isForbidden();
    verify(gradebook, never()).course(anyLong());
  }

  @Test
  void aStudentGetsOnlyTheirOwnGrades() {
    client(controller, STUDENT).get().uri("/api/v1/courses/1/gradebook/me").exchange().expectStatus().isOk();
    verify(gradebook).student(1L, 5L);
  }

  @Test
  void onlyStaffSetWeights() {
    client(controller, STUDENT).put().uri("/api/v1/courses/1/gradebook/weights")
      .bodyValue(Map.of("weights", Map.of("10", 2))).exchange().expectStatus().isForbidden();
    client(controller, ADMIN).put().uri("/api/v1/courses/1/gradebook/weights")
      .bodyValue(Map.of("weights", Map.of("10", 2))).exchange().expectStatus().isOk();
    verify(gradebook).saveWeights(1L, Map.of(10L, 2));
  }
}
