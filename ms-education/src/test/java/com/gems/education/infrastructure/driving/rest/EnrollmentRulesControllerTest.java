package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.EnrollmentRulesUseCase;
import com.gems.education.application.EnrollmentRulesUseCase.Eligibility;
import com.gems.education.domain.entities.AcademicPeriod;
import com.gems.education.domain.entities.EnrollmentRules;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EnrollmentRulesControllerTest extends ControllerTestSupport {
  private final EnrollmentRulesUseCase useCase = mock(EnrollmentRulesUseCase.class);
  private EnrollmentRulesController controller;

  private static final Map<String, Object> PERIOD = Map.of("name", "2026-2", "startsOn", "2026-08-01", "endsOn", "2026-11-30");

  @BeforeEach
  void setUp() {
    givenCourses();
    AcademicPeriod p = new AcademicPeriod(9L, "inst-1", "2026-2", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 11, 30), null);
    when(useCase.periods(any())).thenReturn(Flux.just(p));
    when(useCase.savePeriod(any(), any(), any(), any(), any())).thenReturn(Mono.just(p));
    when(useCase.rules(anyLong())).thenAnswer(inv -> Mono.just(EnrollmentRules.none(inv.getArgument(0))));
    when(useCase.saveRules(anyLong(), any())).thenAnswer(inv -> Mono.just(inv.getArgument(1)));
    when(useCase.eligibility(anyLong(), anyLong(), anyBoolean())).thenAnswer(inv -> Mono.just(new Eligibility(
      inv.getArgument(1), true, List.of(), List.of(), null, null, null, null)));
    com.gems.education.application.PeriodClosingUseCase closing = org.mockito.Mockito.mock(com.gems.education.application.PeriodClosingUseCase.class);
    controller = new EnrollmentRulesController(useCase, access, closing);
  }

  @Test
  void adminsManagePeriodsOfTheirInstitution() {
    client(controller, ADMIN).post().uri("/api/v1/academic-periods").bodyValue(PERIOD).exchange().expectStatus().isCreated();
    verify(useCase).savePeriod(isNull(), eq("inst-1"), eq("2026-2"), eq(LocalDate.of(2026, 8, 1)), eq(LocalDate.of(2026, 11, 30)));

    client(controller, INSTRUCTOR).post().uri("/api/v1/academic-periods").bodyValue(PERIOD).exchange().expectStatus().isForbidden();
    client(controller, STUDENT).get().uri("/api/v1/academic-periods").exchange().expectStatus().isOk();
    verify(useCase).periods("inst-1");
  }

  @Test
  void onlyCourseStaffChangeTheRules() {
    Map<String, Object> rules = Map.of("capacity", 30, "selfEnrollment", false, "prerequisiteIds", List.of(2));
    client(controller, STUDENT).put().uri("/api/v1/courses/1/enrollment-rules").bodyValue(rules).exchange().expectStatus().isForbidden();
    client(controller, OTHER_ADMIN).put().uri("/api/v1/courses/1/enrollment-rules").bodyValue(rules).exchange().expectStatus().isForbidden();
    client(controller, INSTRUCTOR).put().uri("/api/v1/courses/1/enrollment-rules").bodyValue(rules).exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.capacity").isEqualTo(30).jsonPath("$.selfEnrollment").isEqualTo(false);
  }

  @Test
  void studentsAskForTheirOwnEligibility() {
    client(controller, STUDENT).get().uri("/api/v1/courses/eligibility?courseIds=1,2").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.length()").isEqualTo(2);
    verify(useCase).eligibility(5L, 1L, false);
    verify(useCase).eligibility(5L, 2L, false);
  }
}
