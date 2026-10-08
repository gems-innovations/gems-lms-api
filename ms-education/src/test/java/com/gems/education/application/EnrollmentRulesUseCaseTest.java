package com.gems.education.application;

import com.gems.education.TestData;
import com.gems.education.application.exceptions.EnrollmentNotAllowedException;
import com.gems.education.application.gateway.AcademicPeriodGateway;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.gateway.EnrollmentRulesGateway;
import com.gems.education.domain.entities.AcademicPeriod;
import com.gems.education.domain.entities.Enrollment;
import com.gems.education.domain.entities.EnrollmentPolicy;
import com.gems.education.domain.entities.EnrollmentRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static com.gems.education.domain.entities.EnrollmentPolicy.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnrollmentRulesUseCaseTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);

  private final EnrollmentRulesGateway rulesGateway = mock(EnrollmentRulesGateway.class);
  private final AcademicPeriodGateway periods = mock(AcademicPeriodGateway.class);
  private final CourseGateway courses = mock(CourseGateway.class);
  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);
  private final EnrollmentRulesUseCase useCase = new EnrollmentRulesUseCase(rulesGateway, periods, courses, enrollments,
    Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

  private final AcademicPeriod term = new AcademicPeriod(9L, "inst-1", "2026-2", LocalDate.of(2026, 8, 1),
    LocalDate.of(2026, 11, 30), NOW);

  @BeforeEach
  void setUp() {
    when(courses.findHeaderById(anyLongOr())).thenAnswer(inv -> {
      Long id = inv.getArgument(0);
      String inst = id == 99L ? "inst-2" : "inst-1";
      return Mono.just(TestData.course(id, "Curso " + id, "d", "published", inst, NOW, NOW, List.of()));
    });
    when(periods.findById(9L)).thenReturn(Mono.just(term));
    when(rulesGateway.findByInstitution("inst-1")).thenReturn(Flux.empty());
    when(rulesGateway.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(enrollments.findByCourseId(1L)).thenReturn(Flux.just(enrollment(5L, 1L, "active")));
    when(enrollments.findByStudentId(6L)).thenReturn(Flux.just(enrollment(6L, 2L, "completed")));
  }

  @Test
  void studentsMustMeetEveryRuleStaffOnlyTheCapacity() {
    EnrollmentRules rules = new EnrollmentRules(1L, 9L, NOW.plusDays(1), null, 1, false, List.of(2L, 3L));
    var decision = EnrollmentPolicy.check(rules, term, NOW, 1, Set.of(2L), false);
    assertThat(decision.reasons()).containsExactly(SELF_ENROLLMENT_DISABLED, NOT_OPEN_YET, MISSING_PREREQUISITES, FULL);
    assertThat(decision.missingPrerequisites()).containsExactly(3L);
    assertThat(decision.seatsLeft()).isZero();

    assertThat(EnrollmentPolicy.check(rules, term, NOW, 1, Set.of(), true).reasons()).containsExactly(FULL);
    assertThat(EnrollmentPolicy.check(rules, term, NOW, 0, Set.of(), true).allowed()).isTrue();
  }

  @Test
  void withoutItsOwnClosingDateEnrollmentClosesWhenThePeriodEnds() {
    EnrollmentRules rules = new EnrollmentRules(1L, 9L, null, null, null, true, List.of());
    assertThat(EnrollmentPolicy.closesAt(rules, term)).isEqualTo(LocalDate.of(2026, 11, 30).atTime(23, 59, 59, 999_999_999));
    assertThat(EnrollmentPolicy.check(rules, term, LocalDateTime.of(2026, 12, 1, 0, 0), 0, Set.of(), false).reasons())
      .containsExactly(CLOSED);
    assertThat(EnrollmentPolicy.check(EnrollmentRules.none(1L), null, NOW, 500, Set.of(), false).allowed()).isTrue();
  }

  @Test
  void eligibilityUsesEnrollmentsAndCompletedCourses() {
    when(rulesGateway.find(1L)).thenReturn(Mono.just(new EnrollmentRules(1L, 9L, null, null, 2, true, List.of(2L))));

    StepVerifier.create(useCase.eligibility(6L, 1L, false)).assertNext(e -> {
      assertThat(e.allowed()).isTrue();
      assertThat(e.seatsLeft()).isEqualTo(1);
      assertThat(e.period().name()).isEqualTo("2026-2");
    }).verifyComplete();

    when(enrollments.findByStudentId(7L)).thenReturn(Flux.empty());
    StepVerifier.create(useCase.requireAllowed(7L, 1L, false))
      .expectErrorMatches(e -> e instanceof EnrollmentNotAllowedException n && n.getReasons().equals(List.of(MISSING_PREREQUISITES)))
      .verify();
  }

  @Test
  void coursesWithoutRulesAreOpen() {
    when(rulesGateway.find(1L)).thenReturn(Mono.empty());
    when(enrollments.findByStudentId(7L)).thenReturn(Flux.empty());
    StepVerifier.create(useCase.requireAllowed(7L, 1L, false)).verifyComplete();
  }

  @Test
  void rulesAreValidated() {
    StepVerifier.create(useCase.saveRules(1L, rules(List.of(1L)))).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(useCase.saveRules(1L, rules(List.of(99L)))).expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(useCase.saveRules(1L, new EnrollmentRules(1L, null, NOW, NOW.minusDays(1), null, true, List.of())))
      .expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(useCase.saveRules(1L, new EnrollmentRules(1L, null, null, null, 0, true, List.of())))
      .expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(useCase.saveRules(1L, rules(List.of(2L, 2L)))).assertNext(r -> assertThat(r.prerequisiteIds()).containsExactly(2L))
      .verifyComplete();
  }

  @Test
  void prerequisiteCyclesAreRejected() {
    // 2 requires 3, 3 requires 1: making 1 require 2 closes the loop.
    when(rulesGateway.findByInstitution("inst-1")).thenReturn(Flux.just(
      new EnrollmentRules(2L, null, null, null, null, true, List.of(3L)),
      new EnrollmentRules(3L, null, null, null, null, true, List.of(1L))));
    StepVerifier.create(useCase.saveRules(1L, rules(List.of(2L)))).expectError(IllegalArgumentException.class).verify();
    verify(rulesGateway, never()).save(any());
  }

  @Test
  void periodsNeedANameAndOrderedDates() {
    StepVerifier.create(useCase.savePeriod(null, "inst-1", " ", LocalDate.now(), LocalDate.now()))
      .expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(useCase.savePeriod(null, "inst-1", "2027-1", LocalDate.of(2027, 2, 1), LocalDate.of(2027, 1, 1)))
      .expectError(IllegalArgumentException.class).verify();
    StepVerifier.create(useCase.savePeriod(9L, "inst-2", "Otro", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1)))
      .expectError(IllegalArgumentException.class).verify();
  }

  private static EnrollmentRules rules(List<Long> prereqs) {
    return new EnrollmentRules(1L, null, null, null, null, true, prereqs);
  }

  private static Enrollment enrollment(Long studentId, Long courseId, String status) {
    return new Enrollment(studentId * 100 + courseId, studentId, courseId, status, NOW, 0, null);
  }

  private static Long anyLongOr() {
    return org.mockito.ArgumentMatchers.anyLong();
  }
}
