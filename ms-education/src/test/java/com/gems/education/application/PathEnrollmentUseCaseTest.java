package com.gems.education.application;

import com.gems.education.TestData;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.LearningPathGateway;
import com.gems.education.application.gateway.PathEnrollmentGateway;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.LearningPath;
import com.gems.education.domain.entities.LearningPathStep;
import com.gems.education.domain.entities.PathEnrollment;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PathEnrollmentUseCaseTest {
  private final PathEnrollmentGateway gateway = mock(PathEnrollmentGateway.class);
  private final LearningPathGateway paths = mock(LearningPathGateway.class);
  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);
  private final PathEnrollmentUseCase useCase = new PathEnrollmentUseCase(gateway, paths, enrollments);

  @Test
  void progressCountsOnlyRequiredCoursesAndCompletesThePath() {
    LearningPath path = new LearningPath(
      7L, "Ruta", null, "inst-1", LocalDateTime.now(), List.of(
        TestData.course(1L, "A", null, "published", "inst-1", LocalDateTime.now(), LocalDateTime.now(), List.of()), TestData.course(2L, "B", null, "published", "inst-1", LocalDateTime.now(), LocalDateTime.now(), List.of()),
        TestData.course(3L, "C", null, "published", "inst-1", LocalDateTime.now(), LocalDateTime.now(), List.of())));
    path.setSteps(List.of(new LearningPathStep(3L, false, null)));
    PathEnrollment active = new PathEnrollment(1L, 7L, 5L, PathEnrollment.ACTIVE, LocalDateTime.now(), null);
    Enrollment c1 =
      TestData.enrollment(10L, 5L, 1L, LocalDateTime.now(), 100, LocalDateTime.now());
    c1.setStatus("completed");
    Enrollment c2 =
      TestData.enrollment(11L, 5L, 2L, LocalDateTime.now(), 40, null);
    when(enrollments.findByStudentId(5L)).thenReturn(Flux.just(c1, c2));
    when(gateway.findByStudent(5L)).thenReturn(Flux.just(active));
    when(paths.findById(7L)).thenReturn(Mono.just(path));
    when(gateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(useCase.progressOf(5L))
      .assertNext(p -> {
        assertEquals(List.of(1L), p.completedCourseIds());
        assertEquals(2L, p.currentCourseId());
        assertEquals(50, p.overallPercentage());
        assertEquals(PathEnrollment.ACTIVE, p.enrollment().status());
      })
      .verifyComplete();

    c2.setStatus("completed");
    StepVerifier.create(useCase.progressOf(5L))
      .assertNext(p -> {
        assertEquals(100, p.overallPercentage());
        assertEquals("completed", p.enrollment().status());
      })
      .verifyComplete();
  }

  @Test
  void enrollingIsIdempotentAndSkipsDuplicates() {
    PathEnrollment existing = new PathEnrollment(1L, 7L, 5L, PathEnrollment.ACTIVE, LocalDateTime.now(), null);
    when(gateway.find(7L, 5L)).thenReturn(Mono.just(existing));
    when(gateway.find(7L, 6L)).thenReturn(Mono.empty());
    when(gateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    StepVerifier.create(useCase.enroll(7L, Arrays.asList(5L, 6L, 6L, null)))
      .assertNext(e -> assertEquals(1L, e.id()))
      .assertNext(e -> {
        assertEquals(6L, e.studentId());
        assertEquals(PathEnrollment.ACTIVE, e.status());
      })
      .verifyComplete();
    verify(gateway, times(1)).save(any());
  }
}
