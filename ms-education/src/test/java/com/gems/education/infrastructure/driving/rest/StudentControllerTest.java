package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.response.StudentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

/** The legacy student registry is staff-only. */
class StudentControllerTest extends ControllerTestSupport {
  private final RegisterStudentUseCase registerStudentUseCase = mock(RegisterStudentUseCase.class);
  private final GetStudentByIdUseCase getStudentByIdUseCase = mock(GetStudentByIdUseCase.class);
  private final GetAllStudentsUseCase getAllStudentsUseCase = mock(GetAllStudentsUseCase.class);
  private final UpdateStudentUseCase updateStudentUseCase = mock(UpdateStudentUseCase.class);
  private final DeleteStudentUseCase deleteStudentUseCase = mock(DeleteStudentUseCase.class);
  private StudentController controller;

  private final StudentResponse student = new StudentResponse(1L, "María López", "maria@unal.edu.co",
    LocalDate.of(2000, 5, 10), "Colombia", "Bogotá", "CC", "1000000001");

  @BeforeEach
  void setUp() {
    controller = new StudentController(registerStudentUseCase, getStudentByIdUseCase, getAllStudentsUseCase,
      updateStudentUseCase, deleteStudentUseCase, access);
  }

  @Test
  void staffListAndReadStudents() {
    when(getAllStudentsUseCase.execute()).thenReturn(Flux.just(student));
    when(getStudentByIdUseCase.execute(1L)).thenReturn(Mono.just(student));

    client(controller, ADMIN).get().uri("/api/v1/students").exchange().expectStatus().isOk()
      .expectBodyList(StudentResponse.class).hasSize(1);
    client(controller, INSTRUCTOR).get().uri("/api/v1/students/1").exchange().expectStatus().isOk()
      .expectBody().jsonPath("$.email").isEqualTo("maria@unal.edu.co");
  }

  @Test
  void studentsCannotUseTheRegistry() {
    client(controller, STUDENT).get().uri("/api/v1/students").exchange().expectStatus().isForbidden();
    client(controller, STUDENT).get().uri("/api/v1/students/1").exchange().expectStatus().isForbidden();
    client(controller, STUDENT).delete().uri("/api/v1/students/1").exchange().expectStatus().isForbidden();
    verifyNoInteractions(getAllStudentsUseCase, getStudentByIdUseCase, deleteStudentUseCase);
  }

  @Test
  void staffDeleteStudents() {
    when(deleteStudentUseCase.execute(1L)).thenReturn(Mono.empty());

    client(controller, ADMIN).delete().uri("/api/v1/students/1").exchange().expectStatus().isNoContent();
  }
}
