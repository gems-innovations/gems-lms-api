package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.command.CourseCommand;
import com.gems.education.application.response.CourseListResponse;
import com.gems.education.infrastructure.driving.rest.request.CourseRequest;
import com.gems.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourseControllerTest extends ControllerTestSupport {
  private final CreateCourseUseCase createCourseUseCase = mock(CreateCourseUseCase.class);
  private final UpdateCourseUseCase updateCourseUseCase = mock(UpdateCourseUseCase.class);
  private final DeleteCourseUseCase deleteCourseUseCase = mock(DeleteCourseUseCase.class);
  private final GetCoursesByInstitutionUseCase getCoursesByInstitutionUseCase = mock(GetCoursesByInstitutionUseCase.class);
  private final GetAllCoursesUseCase getAllCoursesUseCase = mock(GetAllCoursesUseCase.class);
  private CourseController controller;

  @BeforeEach
  void setUp() {
    givenCourses();
    controller = new CourseController(createCourseUseCase, getCourseByIdUseCase, updateCourseUseCase,
      deleteCourseUseCase, getCoursesByInstitutionUseCase, getAllCoursesUseCase, access, studentView);
  }

  private WebTestClient as(AuthenticatedUser caller) {
    return client(controller, caller);
  }

  private static CourseListResponse page() {
    return new CourseListResponse(List.of(course(1L, "published", "inst-1")), 1, 1, 10, 1, false, false);
  }

  @Test
  void listIsScopedToTheCallersInstitutionAndStudentsOnlySeePublished() {
    when(getAllCoursesUseCase.execute(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(Mono.just(page()));

    as(STUDENT).get().uri("/api/v1/courses?institutionId=inst-2&status=draft").exchange().expectStatus().isOk();

    verify(getAllCoursesUseCase).execute(null, "published", null, "inst-1", 1, 10);
  }

  @Test
  void superAdminListsAnyInstitution() {
    when(getAllCoursesUseCase.execute(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(Mono.just(page()));

    as(SUPER_ADMIN).get().uri("/api/v1/courses?institutionId=inst-2").exchange().expectStatus().isOk();

    verify(getAllCoursesUseCase).execute(null, null, null, "inst-2", 1, 10);
  }

  @Test
  void studentsReceiveQuizzesWithoutAnswerKeys() {
    String body = as(STUDENT).get().uri("/api/v1/courses/1").exchange()
      .expectStatus().isOk()
      .expectBody(String.class).returnResult().getResponseBody();

    assertEquals(false, body.contains("correctAnswer"));
    assertEquals(false, body.contains("explanation"));
  }

  @Test
  void staffReceiveQuizzesWithAnswerKeys() {
    String body = as(INSTRUCTOR).get().uri("/api/v1/courses/1").exchange()
      .expectStatus().isOk()
      .expectBody(String.class).returnResult().getResponseBody();

    assertEquals(true, body.contains("correctAnswer"));
  }

  @Test
  void studentsCannotSeeDraftCourses() {
    when(getCourseByIdUseCase.execute(2L)).thenReturn(Mono.just(course(2L, "draft", "inst-1")));

    as(STUDENT).get().uri("/api/v1/courses/2").exchange().expectStatus().isForbidden();
  }

  @Test
  void otherInstitutionsCannotSeeTheCourse() {
    as(OTHER_ADMIN).get().uri("/api/v1/courses/1").exchange().expectStatus().isForbidden();
  }

  @Test
  void instructorCreatesCoursesInTheirInstitutionByDefault() {
    when(createCourseUseCase.execute(any())).thenReturn(Mono.just(course(3L, "draft", "inst-1")));
    CourseRequest request = new CourseRequest("Nuevo", "Desc", "draft", "beginner", List.of(), null, null, null, List.of());

    as(INSTRUCTOR).post().uri("/api/v1/courses").contentType(MediaType.APPLICATION_JSON).bodyValue(request)
      .exchange().expectStatus().isCreated();

    ArgumentCaptor<CourseCommand> command = ArgumentCaptor.forClass(CourseCommand.class);
    verify(createCourseUseCase).execute(command.capture());
    assertEquals("inst-1", command.getValue().getInstitutionId());
  }

  @Test
  void studentsCannotCreateCourses() {
    CourseRequest request = new CourseRequest("Nuevo", "Desc", "draft", "beginner", List.of(), null, null, "inst-1", List.of());

    as(STUDENT).post().uri("/api/v1/courses").contentType(MediaType.APPLICATION_JSON).bodyValue(request)
      .exchange().expectStatus().isForbidden();
    verifyNoInteractions(createCourseUseCase);
  }

  @Test
  void staffCannotMoveACourseToAnotherInstitution() {
    CourseRequest request = new CourseRequest("X", null, null, null, null, null, null, "inst-2", null);

    as(ADMIN).put().uri("/api/v1/courses/1").contentType(MediaType.APPLICATION_JSON).bodyValue(request)
      .exchange().expectStatus().isForbidden();
    verifyNoInteractions(updateCourseUseCase);
  }

  @Test
  void staffUpdateAndDeleteTheirCourses() {
    when(updateCourseUseCase.execute(eq(1L), any())).thenReturn(Mono.just(course(1L, "published", "inst-1")));
    when(deleteCourseUseCase.execute(1L)).thenReturn(Mono.empty());
    CourseRequest request = new CourseRequest("Docker 2", null, null, null, null, null, null, null, null);

    as(INSTRUCTOR).put().uri("/api/v1/courses/1").contentType(MediaType.APPLICATION_JSON).bodyValue(request)
      .exchange().expectStatus().isOk();
    as(ADMIN).delete().uri("/api/v1/courses/1").exchange().expectStatus().isNoContent();
  }

  @Test
  void otherInstitutionsCannotDeleteTheCourse() {
    as(OTHER_ADMIN).delete().uri("/api/v1/courses/1").exchange().expectStatus().isForbidden();
    verifyNoInteractions(deleteCourseUseCase);
  }
}
