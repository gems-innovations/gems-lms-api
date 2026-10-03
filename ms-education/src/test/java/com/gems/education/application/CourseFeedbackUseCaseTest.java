package com.gems.education.application;

import com.gems.education.TestData;
import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.application.gateway.CourseFeedbackGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.domain.entities.CourseFeedback.Review;
import com.gems.education.domain.entities.CourseFeedback.Survey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseFeedbackUseCaseTest {
  private final CourseFeedbackGateway gateway = mock(CourseFeedbackGateway.class);
  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);
  private final CourseFeedbackUseCase useCase = new CourseFeedbackUseCase(gateway, enrollments);

  @BeforeEach
  void setUp() {
    when(enrollments.findByStudentIdAndCourseId(5L, 1L))
      .thenReturn(Mono.just(TestData.enrollment(20L, 5L, 1L, LocalDateTime.now(), 0, null)));
    when(enrollments.findByStudentIdAndCourseId(6L, 1L)).thenReturn(Mono.empty());
    when(gateway.saveSurvey(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(gateway.saveResponse(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(gateway.saveReview(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
  }

  @Test
  void savingASurveyKeepsItsIdAndDefaultsTheTitle() {
    when(gateway.findSurvey(1L)).thenReturn(Mono.just(new Survey(4L, 1L, "Old", null, "[]", false, LocalDateTime.now())));

    StepVerifier.create(useCase.saveSurvey(1L, " ", null, null, true))
      .assertNext(s -> {
        assertEquals(4L, s.id());
        assertEquals("Encuesta del curso", s.title());
        assertEquals("[]", s.sections());
      })
      .verifyComplete();
  }

  @Test
  void onlyEnrolledStudentsAnswerAnOpenSurvey() {
    when(gateway.findSurvey(1L)).thenReturn(Mono.just(new Survey(4L, 1L, "S", null, "[]", false, LocalDateTime.now())));
    when(gateway.findResponse(4L, 5L)).thenReturn(Mono.empty());

    StepVerifier.create(useCase.respond(1L, 6L, "[]"))
      .expectErrorMatches(e -> e instanceof CourseActivityException c && c.getCode().equals("NOT_ENROLLED")).verify();
    StepVerifier.create(useCase.respond(1L, 5L, "[]"))
      .expectErrorMatches(e -> e instanceof CourseActivityException c
        && c.getCode().equals(CourseFeedbackUseCase.SURVEY_CLOSED)).verify();

    when(gateway.findSurvey(1L)).thenReturn(Mono.just(new Survey(4L, 1L, "S", null, "[]", true, LocalDateTime.now())));
    StepVerifier.create(useCase.respond(1L, 5L, "[{\"questionId\":\"q1\",\"value\":9}]"))
      .assertNext(r -> assertEquals(4L, r.surveyId()))
      .verifyComplete();
  }

  @Test
  void aSecondReviewReplacesTheFirst() {
    LocalDateTime created = LocalDateTime.of(2026, 1, 1, 0, 0);
    when(gateway.findReview(1L, 5L)).thenReturn(Mono.just(new Review(3L, 1L, 5L, 2, "Meh", created, created)));

    StepVerifier.create(useCase.review(1L, 5L, 5, "  Mucho mejor "))
      .assertNext(r -> {
        assertEquals(3L, r.id());
        assertEquals(5, r.rating());
        assertEquals("Mucho mejor", r.comment());
        assertEquals(created, r.createdAt());
      })
      .verifyComplete();
  }

  @Test
  void ratingsOutsideOneToFiveAreRejected() {
    StepVerifier.create(useCase.review(1L, 5L, 0, "x")).expectError(IllegalArgumentException.class).verify();
    verifyNoInteractions(gateway);
  }
}
