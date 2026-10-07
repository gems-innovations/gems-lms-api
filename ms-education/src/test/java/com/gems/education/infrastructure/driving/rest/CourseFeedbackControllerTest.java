package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.CourseFeedbackUseCase;
import com.gems.education.application.exceptions.CourseActivityException;
import com.gems.education.domain.entities.CourseFeedback.Review;
import com.gems.education.domain.entities.CourseFeedback.Survey;
import com.gems.education.domain.entities.CourseFeedback.SurveyResponse;
import com.gems.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourseFeedbackControllerTest extends ControllerTestSupport {
  private final CourseFeedbackUseCase feedback = mock(CourseFeedbackUseCase.class);
  private CourseFeedbackController controller;

  private static final String SECTIONS = "[{\"id\":\"s1\",\"title\":\"Contenido\",\"questions\":[{\"id\":\"q1\",\"type\":\"scale\",\"label\":\"Claridad\"}]}]";

  private static Survey survey(boolean published) {
    return new Survey(4L, 1L, "Encuesta", null, SECTIONS, published, LocalDateTime.now());
  }

  @BeforeEach
  void setUp() {
    givenCourses();
    controller = new CourseFeedbackController(feedback, access);
  }

  private WebTestClient as(AuthenticatedUser caller) {
    return client(controller, caller);
  }

  @Test
  void studentsOnlySeeAPublishedSurvey() {
    when(feedback.survey(1L)).thenReturn(Mono.just(survey(false)));

    as(STUDENT).get().uri("/api/v1/courses/1/survey").exchange().expectStatus().isNotFound();
    as(INSTRUCTOR).get().uri("/api/v1/courses/1/survey").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.sections[0].questions[0].type").isEqualTo("scale")
      .jsonPath("$.isPublished").isEqualTo(false);
  }

  @Test
  void onlyStaffOfTheCourseEditTheSurvey() {
    when(feedback.saveSurvey(eq(1L), any(), any(), anyString(), anyBoolean())).thenReturn(Mono.just(survey(true)));
    String body = "{\"title\":\"Encuesta\",\"sections\":" + SECTIONS + ",\"isPublished\":true}";

    as(STUDENT).put().uri("/api/v1/courses/1/survey").contentType(MediaType.APPLICATION_JSON).bodyValue(body)
      .exchange().expectStatus().isForbidden();
    as(OTHER_ADMIN).put().uri("/api/v1/courses/1/survey").contentType(MediaType.APPLICATION_JSON).bodyValue(body)
      .exchange().expectStatus().isForbidden();
    as(INSTRUCTOR).put().uri("/api/v1/courses/1/survey").contentType(MediaType.APPLICATION_JSON).bodyValue(body)
      .exchange().expectStatus().isOk();
    verify(feedback).saveSurvey(eq(1L), eq("Encuesta"), isNull(), contains("\"q1\""), eq(true));
  }

  @Test
  void studentsAnswerAsThemselvesAndStaffReadTheResponses() {
    when(feedback.respond(eq(1L), eq(5L), anyString())).thenReturn(Mono.just(
      new SurveyResponse(9L, 4L, 1L, 5L, "[{\"questionId\":\"q1\",\"value\":8}]", LocalDateTime.now())));
    when(feedback.responses(1L)).thenReturn(Flux.just(
      new SurveyResponse(9L, 4L, 1L, 5L, "[{\"questionId\":\"q1\",\"value\":8}]", LocalDateTime.now())));

    as(STUDENT).post().uri("/api/v1/courses/1/survey/responses").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"answers\":[{\"questionId\":\"q1\",\"value\":8}]}")
      .exchange().expectStatus().isCreated()
      .expectBody().jsonPath("$.answers[0].value").isEqualTo(8);
    as(STUDENT).get().uri("/api/v1/courses/1/survey/responses").exchange().expectStatus().isForbidden();
    as(ADMIN).get().uri("/api/v1/courses/1/survey/responses").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$[0].studentId").isEqualTo(5);
  }

  @Test
  void studentsNotEnrolledCannotAnswer() {
    when(feedback.respond(anyLong(), anyLong(), anyString())).thenReturn(Mono.error(
      new CourseActivityException(CourseActivityException.NOT_ENROLLED, "not enrolled")));

    as(STUDENT).post().uri("/api/v1/courses/1/survey/responses").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"answers\":[]}")
      .exchange().expectStatus().isForbidden();
  }

  @Test
  void onlyStudentsReviewAndRatingsMustBeOneToFive() {
    when(feedback.review(1L, 5L, 4, "Muy bueno")).thenReturn(Mono.just(
      new Review(3L, 1L, 5L, 4, "Muy bueno", LocalDateTime.now(), LocalDateTime.now())));

    as(STUDENT).put().uri("/api/v1/courses/1/review").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"rating\":4,\"comment\":\"Muy bueno\"}")
      .exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.rating").isEqualTo(4);
    as(STUDENT).put().uri("/api/v1/courses/1/review").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"rating\":6}")
      .exchange().expectStatus().isBadRequest();
    as(INSTRUCTOR).put().uri("/api/v1/courses/1/review").contentType(MediaType.APPLICATION_JSON)
      .bodyValue("{\"rating\":5}")
      .exchange().expectStatus().isForbidden();
  }

  @Test
  void reviewsAreReadableByWhoeverSeesTheCourse() {
    when(feedback.reviews(1L)).thenReturn(Flux.just(
      new Review(3L, 1L, 5L, 4, "Muy bueno", LocalDateTime.now(), LocalDateTime.now())));

    as(STUDENT).get().uri("/api/v1/courses/1/reviews").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$[0].comment").isEqualTo("Muy bueno");
    as(OTHER_ADMIN).get().uri("/api/v1/courses/1/reviews").exchange().expectStatus().isForbidden();
  }
}
