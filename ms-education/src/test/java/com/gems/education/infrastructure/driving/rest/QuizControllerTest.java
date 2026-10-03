package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.gateway.CourseGateway;
import com.gems.education.application.gateway.EnrollmentGateway;
import com.gems.education.application.response.QuestionResponse;
import com.gems.education.application.response.QuizGradingResponse;
import com.gems.education.application.response.QuizResponse;
import com.gems.education.infrastructure.driving.rest.request.QuestionRequest;
import com.gems.education.infrastructure.driving.rest.request.QuizRequest;
import com.gems.education.infrastructure.driving.rest.request.QuizSubmissionRequest;
import com.gems.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QuizControllerTest extends ControllerTestSupport {
  private final CreateQuizUseCase createQuizUseCase = mock(CreateQuizUseCase.class);
  private final GetQuizByLessonUseCase getQuizByLessonUseCase = mock(GetQuizByLessonUseCase.class);
  private final GetQuizByIdUseCase getQuizByIdUseCase = mock(GetQuizByIdUseCase.class);
  private final UpdateQuizUseCase updateQuizUseCase = mock(UpdateQuizUseCase.class);
  private final DeleteQuizUseCase deleteQuizUseCase = mock(DeleteQuizUseCase.class);
  private final SubmitQuizUseCase submitQuizUseCase = mock(SubmitQuizUseCase.class);
  private QuizController controller;
  private final CourseGateway courses = mock(CourseGateway.class);
  private final EnrollmentGateway enrollments = mock(EnrollmentGateway.class);

  private final QuizResponse quiz = new QuizResponse(1L, 100L, "Docker", 60,
    List.of(new QuestionResponse(1L, 1L, "¿Qué es una imagen?", List.of("A", "B"), "A")));

  @BeforeEach
  void setUp() {
    givenCourses();
    when(courses.findCourseIdByLessonId(100L)).thenReturn(Mono.just(1L));
    when(enrollments.existsByStudentIdAndCourseId(STUDENT.userId(), 1L)).thenReturn(Mono.just(true));
    when(getQuizByIdUseCase.execute(1L)).thenReturn(Mono.just(quiz));
    controller = new QuizController(createQuizUseCase, getQuizByLessonUseCase, getQuizByIdUseCase,
      updateQuizUseCase, deleteQuizUseCase, submitQuizUseCase, access, studentView,
      new LessonAccess(courses, enrollments, access));
  }

  private WebTestClient as(AuthenticatedUser caller) {
    return client(controller, caller);
  }

  @Test
  void studentsDoNotReceiveTheCorrectOption() {
    as(STUDENT).get().uri("/api/v1/quizzes/1").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.questions[0].correctOption").doesNotExist();
  }

  @Test
  void staffReceiveTheCorrectOption() {
    as(INSTRUCTOR).get().uri("/api/v1/quizzes/1").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.questions[0].correctOption").isEqualTo("A");
  }

  @Test
  void onlyStaffWriteQuizzes() {
    when(createQuizUseCase.execute(any())).thenReturn(Mono.just(quiz));
    QuizRequest request = new QuizRequest(100L, "Docker", 60, List.of(new QuestionRequest("¿?", List.of("A", "B"), "A")));

    as(STUDENT).post().uri("/api/v1/quizzes").contentType(MediaType.APPLICATION_JSON).bodyValue(request)
      .exchange().expectStatus().isForbidden();
    as(INSTRUCTOR).post().uri("/api/v1/quizzes").contentType(MediaType.APPLICATION_JSON).bodyValue(request)
      .exchange().expectStatus().isCreated();
    as(STUDENT).delete().uri("/api/v1/quizzes/1").exchange().expectStatus().isForbidden();
  }

  @Test
  void studentsSubmitOnlyTheirOwnAnswers() {
    when(submitQuizUseCase.execute(eq(1L), any())).thenReturn(Mono.just(new QuizGradingResponse(100, true, 1, 1)));
    List<QuizSubmissionRequest.AnswerRequest> answers = List.of(new QuizSubmissionRequest.AnswerRequest(1L, "A"));

    as(STUDENT).post().uri("/api/v1/quizzes/1/submit").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(new QuizSubmissionRequest(STUDENT.userId(), answers))
      .exchange().expectStatus().isOk();
    as(STUDENT).post().uri("/api/v1/quizzes/1/submit").contentType(MediaType.APPLICATION_JSON)
      .bodyValue(new QuizSubmissionRequest(99L, answers))
      .exchange().expectStatus().isForbidden();
  }

  @Test
  void staffOfAnotherInstitutionCannotReadWriteOrDeleteQuizzes() {
    QuizRequest request = new QuizRequest(100L, "Docker", 60, List.of());
    as(OTHER_ADMIN).get().uri("/api/v1/quizzes/1").exchange().expectStatus().isForbidden();
    as(OTHER_ADMIN).post().uri("/api/v1/quizzes").bodyValue(request).exchange().expectStatus().isForbidden();
    as(OTHER_ADMIN).put().uri("/api/v1/quizzes/1").bodyValue(request).exchange().expectStatus().isForbidden();
    as(OTHER_ADMIN).delete().uri("/api/v1/quizzes/1").exchange().expectStatus().isForbidden();
    verifyNoInteractions(createQuizUseCase, updateQuizUseCase, deleteQuizUseCase);
  }

  @Test
  void aStudentMustBeEnrolledToSubmit() {
    when(enrollments.existsByStudentIdAndCourseId(STUDENT.userId(), 1L)).thenReturn(Mono.just(false));
    as(STUDENT).post().uri("/api/v1/quizzes/1/submit")
      .bodyValue(new QuizSubmissionRequest(STUDENT.userId(),
        List.of(new QuizSubmissionRequest.AnswerRequest(1L, "A"))))
      .exchange().expectStatus().isForbidden();
    verifyNoInteractions(submitQuizUseCase);
  }

  @Test
  void updatingCannotMoveAQuizToAnotherInstitution() {
    when(courses.findCourseIdByLessonId(200L)).thenReturn(Mono.just(2L));
    when(getCourseByIdUseCase.execute(2L)).thenReturn(Mono.just(course(2L, "published", "inst-2")));
    as(INSTRUCTOR).put().uri("/api/v1/quizzes/1")
      .bodyValue(new QuizRequest(200L, "Docker", 60, List.of()))
      .exchange().expectStatus().isForbidden();
    verifyNoInteractions(updateQuizUseCase);
  }
}
