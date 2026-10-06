package com.gems.education.application;

import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.application.gateway.AcademicPeriodGateway;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.EnrollmentRulesGateway;
import com.gems.education.domain.entities.AcademicPeriod;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.EnrollmentRules;
import com.gems.education.domain.entities.PeriodGradeRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PeriodClosingUseCaseTest {
  private final AcademicPeriodGateway periods = mock(AcademicPeriodGateway.class);
  private final EnrollmentRulesGateway rules = mock(EnrollmentRulesGateway.class);
  private final CourseGateway courses = mock(CourseGateway.class);
  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);
  private final GradebookUseCase gradebook = mock(GradebookUseCase.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-12-01T10:00:00Z"), ZoneOffset.UTC);
  private final PeriodClosingUseCase useCase = new PeriodClosingUseCase(periods, rules, courses, enrollments, gradebook, clock);

  private final AcademicPeriod open = new AcademicPeriod(9L, "inst-1", "2026-2", LocalDate.of(2026, 8, 1),
    LocalDate.of(2026, 11, 30), null);

  private static EnrollmentRules inPeriod(Long courseId, Long periodId) {
    return new EnrollmentRules(courseId, periodId, null, null, null, true, List.of());
  }

  @Test
  void closingFreezesFinalGradesOfThePeriodCoursesOnly() {
    when(rules.findByInstitution("inst-1")).thenReturn(Flux.just(inPeriod(1L, 9L), inPeriod(2L, 7L)));
    when(courses.findById(1L)).thenReturn(Mono.empty());
    when(enrollments.findByCourseId(1L)).thenReturn(Flux.just(
      new Enrollment(100L, 50L, 1L, "active", LocalDateTime.now(clock), 80, null),
      new Enrollment(101L, 51L, 1L, "active", LocalDateTime.now(clock), 30, null)));
    when(gradebook.course(1L)).thenReturn(Mono.just(new GradebookUseCase.Gradebook(1L, List.of(), List.of(
      new GradebookUseCase.Row(50L, 100L, "active", List.of(), 90.0, 75.0),
      new GradebookUseCase.Row(51L, 101L, "active", List.of(), 70.0, 40.0)))));
    when(periods.replaceRecords(anyLong(), any())).thenReturn(Mono.empty());
    AcademicPeriod closed = new AcademicPeriod(9L, "inst-1", "2026-2", open.startsOn(), open.endsOn(), null,
      LocalDateTime.now(clock), 3L);
    when(periods.setClosed(eq(9L), any(), eq(3L))).thenReturn(Mono.just(closed));

    StepVerifier.create(useCase.close(open, 3L))
      .assertNext(summary -> {
        assertEquals(1, summary.courses());
        assertEquals(2, summary.students());
        assertEquals(1, summary.passed());
        assertEquals(1, summary.failed());
      })
      .verifyComplete();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<PeriodGradeRecord>> saved = ArgumentCaptor.forClass(List.class);
    verify(periods).replaceRecords(eq(9L), saved.capture());
    PeriodGradeRecord first = saved.getValue().get(0);
    assertEquals(75.0, first.finalGrade());
    assertEquals(80, first.progress());
    verify(gradebook, never()).course(2L);
  }

  @Test
  void aClosedPeriodCannotBeClosedAgain() {
    AcademicPeriod closed = new AcademicPeriod(9L, "inst-1", "2026-2", open.startsOn(), open.endsOn(), null,
      LocalDateTime.now(clock), 3L);
    StepVerifier.create(useCase.close(closed, 3L)).expectError(IllegalArgumentException.class).verify();
  }

  @Test
  void activityInACourseOfAClosedPeriodIsRejected() {
    when(rules.find(1L)).thenReturn(Mono.just(inPeriod(1L, 9L)));
    when(periods.findById(9L)).thenReturn(Mono.just(new AcademicPeriod(9L, "inst-1", "2026-2", open.startsOn(),
      open.endsOn(), null, LocalDateTime.now(clock), 3L)));
    StepVerifier.create(useCase.requireOpen(1L))
      .expectErrorMatches(e -> e instanceof CourseActivityException c && CourseActivityException.PERIOD_CLOSED.equals(c.getCode()))
      .verify();

    when(periods.findById(9L)).thenReturn(Mono.just(open));
    StepVerifier.create(useCase.requireOpen(1L)).verifyComplete();
  }
}
