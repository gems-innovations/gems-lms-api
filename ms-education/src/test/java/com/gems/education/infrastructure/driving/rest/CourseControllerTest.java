package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.command.CourseCommand;
import com.gems.education.application.response.CourseResponse;
import com.gems.education.infrastructure.driving.rest.request.CourseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CourseControllerTest {

  private CreateCourseUseCase createCourseUseCase;
  private GetCourseByIdUseCase getCourseByIdUseCase;
  private UpdateCourseUseCase updateCourseUseCase;
  private DeleteCourseUseCase deleteCourseUseCase;
  private GetCoursesByInstitutionUseCase getCoursesByInstitutionUseCase;
  private GetAllCoursesUseCase getAllCoursesUseCase;
  private CourseController controller;

  @BeforeEach
  void setUp() {
    createCourseUseCase = mock(CreateCourseUseCase.class);
    getCourseByIdUseCase = mock(GetCourseByIdUseCase.class);
    updateCourseUseCase = mock(UpdateCourseUseCase.class);
    deleteCourseUseCase = mock(DeleteCourseUseCase.class);
    getCoursesByInstitutionUseCase = mock(GetCoursesByInstitutionUseCase.class);
    getAllCoursesUseCase = mock(GetAllCoursesUseCase.class);

    controller = new CourseController(
      createCourseUseCase,
      getCourseByIdUseCase,
      updateCourseUseCase,
      deleteCourseUseCase,
      getCoursesByInstitutionUseCase,
      getAllCoursesUseCase
    );
  }

  private CourseRequest buildRequest() {
    return new CourseRequest(
      "Java Course",
      "Java fundamentals",
      "DRAFT",
      "inst-1",
      List.of()
    );
  }

  private CourseResponse buildResponse() {
    return new CourseResponse(
      1L,
      "Java Course",
      "Java fundamentals",
      "DRAFT",
      "inst-1",
      LocalDateTime.now(),
      LocalDateTime.now(),
      List.of()
    );
  }

  @Test
  void shouldGetAllCourses() {
    CourseResponse response = buildResponse();
    when(getAllCoursesUseCase.execute()).thenReturn(Flux.just(response));

    StepVerifier.create(controller.getAllCourses())
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        StepVerifier.create(entity.getBody())
          .assertNext(res -> assertEquals("Java Course", res.getTitle()))
          .verifyComplete();
      })
      .verifyComplete();

    verify(getAllCoursesUseCase).execute();
  }

  @Test
  void shouldCreateCourse() {
    CourseResponse response = buildResponse();
    when(createCourseUseCase.execute(any(CourseCommand.class))).thenReturn(Mono.just(response));

    StepVerifier.create(controller.createCourse(buildRequest()))
      .assertNext(entity -> {
        assertEquals(201, entity.getStatusCode().value());
        assertEquals("Java Course", entity.getBody().getTitle());
      })
      .verifyComplete();

    verify(createCourseUseCase).execute(any(CourseCommand.class));
  }

  @Test
  void shouldGetCourseById() {
    CourseResponse response = buildResponse();
    when(getCourseByIdUseCase.execute(1L)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.getCourseById(1L))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals("Java Course", entity.getBody().getTitle());
      })
      .verifyComplete();

    verify(getCourseByIdUseCase).execute(1L);
  }

  @Test
  void shouldUpdateCourse() {
    CourseResponse response = buildResponse();
    when(updateCourseUseCase.execute(eq(1L), any(CourseCommand.class))).thenReturn(Mono.just(response));

    StepVerifier.create(controller.updateCourse(1L, buildRequest()))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals("Java Course", entity.getBody().getTitle());
      })
      .verifyComplete();

    verify(updateCourseUseCase).execute(eq(1L), any(CourseCommand.class));
  }

  @Test
  void shouldDeleteCourse() {
    when(deleteCourseUseCase.execute(1L)).thenReturn(Mono.empty());

    StepVerifier.create(controller.deleteCourse(1L))
      .assertNext(entity -> {
        assertEquals(204, entity.getStatusCode().value());
      })
      .verifyComplete();

    verify(deleteCourseUseCase).execute(1L);
  }

  @Test
  void shouldGetCoursesByInstitution() {
    CourseResponse response = buildResponse();
    when(getCoursesByInstitutionUseCase.execute("inst-1")).thenReturn(Flux.just(response));

    StepVerifier.create(controller.getCoursesByInstitution("inst-1"))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        StepVerifier.create(entity.getBody())
          .assertNext(res -> assertEquals("Java Course", res.getTitle()))
          .verifyComplete();
      })
      .verifyComplete();

    verify(getCoursesByInstitutionUseCase).execute("inst-1");
  }
}
