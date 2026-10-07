package com.gems.education.infrastructure.driving.rest;

import com.gems.education.infrastructure.driven.auth.InstitutionMembers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gems.education.application.GetCourseByIdUseCase;
import com.gems.education.application.GetEnrollmentByIdUseCase;
import com.gems.education.application.GetLearningPathByIdUseCase;
import com.gems.education.application.response.ContentResponse;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.application.response.LessonResponse;
import com.gems.education.application.response.ModuleResponse;
import com.gems.shared.security.AuthenticatedUser;
import com.gems.shared.security.SecurityExceptionAdvice;
import org.mockito.Mockito;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

/** Shared fixtures for the education controller tests. */
abstract class ControllerTestSupport {
  static final AuthenticatedUser SUPER_ADMIN = new AuthenticatedUser(1L, "SUPER_ADMIN", null);
  static final AuthenticatedUser ADMIN = new AuthenticatedUser(2L, "ADMIN", "inst-1");
  static final AuthenticatedUser INSTRUCTOR = new AuthenticatedUser(3L, "INSTRUCTOR", "inst-1");
  static final AuthenticatedUser STUDENT = new AuthenticatedUser(5L, "STUDENT", "inst-1");
  static final AuthenticatedUser OTHER_ADMIN = new AuthenticatedUser(7L, "ADMIN", "inst-2");

  /** A quiz block whose JSON carries the answer keys students must never receive. */
  static final String QUIZ_VALUE = "{\"title\":\"Quiz\",\"questions\":[{\"id\":\"q1\",\"type\":\"true-false\","
    + "\"question\":\"¿Docker usa contenedores?\",\"correctAnswer\":true,\"explanation\":\"Sí\"}]}";

  final ObjectMapper mapper = new ObjectMapper();
  final GetCourseByIdUseCase getCourseByIdUseCase = Mockito.mock(GetCourseByIdUseCase.class);
  final GetEnrollmentByIdUseCase getEnrollmentByIdUseCase = Mockito.mock(GetEnrollmentByIdUseCase.class);
  final GetLearningPathByIdUseCase getLearningPathByIdUseCase = Mockito.mock(GetLearningPathByIdUseCase.class);
  final EducationAccess access = new EducationAccess(getCourseByIdUseCase, getEnrollmentByIdUseCase, getLearningPathByIdUseCase);
  final StudentView studentView = new StudentView(mapper);
  /** Every user belongs to the institution unless a test says otherwise. */
  final InstitutionMembers members = Mockito.mock(InstitutionMembers.class);

  {
    lenient().when(members.requireMembers(any(), any())).thenReturn(Mono.empty());
  }

  /** Every course lookup resolves to a published course of inst-1 unless a test says otherwise. */
  void givenCourses() {
    lenient().when(getCourseByIdUseCase.execute(anyLong()))
      .thenAnswer(inv -> Mono.just(course(inv.getArgument(0), "published", "inst-1")));
  }

  static CourseResponse course(Long id, String status, String institutionId) {
    ContentResponse quiz = new ContentResponse(10L, 100L, "quiz", QUIZ_VALUE, 1);
    LessonResponse lesson = new LessonResponse(100L, 1000L, "Lesson", 1, List.of(quiz));
    ModuleResponse module = new ModuleResponse(1000L, id, "Module", 1, List.of(lesson));
    return new CourseResponse(id, "Docker", "Contenedores", status, "beginner", List.of("Docker"), null,
      "Andrés Torres", institutionId, 20, 1, 0, 0, null, 0, null, LocalDateTime.now(), LocalDateTime.now(),
      List.of(module));
  }

  static WebTestClient client(Object controller, AuthenticatedUser caller) {
    return WebTestClient.bindToController(controller)
      .webFilter(TestSecurity.authenticatedAs(caller))
      .controllerAdvice(new GlobalExceptionHandler(), new SecurityExceptionAdvice())
      .build();
  }
}
