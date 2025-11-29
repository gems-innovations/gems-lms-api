package com.gems.education.infrastructure.driving.rest;

import com.gems.education.application.*;
import com.gems.education.application.command.StudentCommand;
import com.gems.education.application.response.StudentResponse;
import com.gems.education.infrastructure.driving.rest.request.StudentRequest;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentControllerTest {

  private RegisterStudentUseCase registerUC;
  private GetStudentByIdUseCase getByIdUC;
  private GetAllStudentsUseCase getAllUC;
  private UpdateStudentUseCase updateUC;
  private DeleteStudentUseCase deleteUC;
  private StudentController controller;

  @BeforeEach
  void setup() {
    registerUC = mock(RegisterStudentUseCase.class);
    getByIdUC = mock(GetStudentByIdUseCase.class);
    getAllUC = mock(GetAllStudentsUseCase.class);
    updateUC = mock(UpdateStudentUseCase.class);
    deleteUC = mock(DeleteStudentUseCase.class);

    controller = new StudentController(registerUC, getByIdUC, getAllUC, updateUC, deleteUC);
  }

  private StudentRequest request() {
    return new StudentRequest(
      "Juan",
      "juan@example.com",
      LocalDate.of(2000, 1, 1),
      "Colombia",
      "Medellín",
      "CC",
      "123456"
    );
  }

  @Test
  void shouldRegisterStudent() {
    StudentResponse response = new StudentResponse(
      1L, "Juan", "juan@example.com",
      LocalDate.of(2000, 1, 1),
      "Colombia","Medellín","CC","123456"
    );

    when(registerUC.execute(any(StudentCommand.class)))
      .thenReturn(Mono.just(response));

    StepVerifier.create(controller.registerStudent(request()))
      .assertNext(entity -> {
        assertEquals(201, entity.getStatusCode().value());
        assertEquals("Juan", entity.getBody().getName());
      })
      .verifyComplete();

    ArgumentCaptor<StudentCommand> captor = ArgumentCaptor.forClass(StudentCommand.class);
    verify(registerUC).execute(captor.capture());
    assertEquals("Juan", captor.getValue().getName());
  }

  @Test
  void shouldGetAllStudents() {
    StudentResponse r1 = new StudentResponse(
      1L, "Juan","juan@example.com",
      LocalDate.of(2000,1,1),
      "Colombia","Medellín","CC","123456"
    );

    when(getAllUC.execute()).thenReturn(Flux.just(r1));

    StepVerifier.create(controller.getAllStudents())
      .assertNext(r -> assertEquals("Juan", r.getName()))
      .verifyComplete();
  }

  @Test
  void shouldUpdateStudent() {
    StudentResponse updated = new StudentResponse(
      1L,"Juan","juan@example.com",
      LocalDate.of(2000,1,1),
      "Colombia","Medellín","CC","123456"
    );

    when(updateUC.execute(eq(1L), any(StudentCommand.class)))
      .thenReturn(Mono.just(updated));

    StepVerifier.create(controller.updateStudent(1L, request()))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals("Juan", entity.getBody().getName());
      })
      .verifyComplete();
  }

  @Test
  void shouldGetStudentById() {
    StudentResponse response = new StudentResponse(
      1L,"Juan","juan@example.com",
      LocalDate.of(2000,1,1),
      "Colombia","Medellín","CC","123456"
    );

    when(getByIdUC.execute(1L)).thenReturn(Mono.just(response));

    StepVerifier.create(controller.getStudentById(1L))
      .assertNext(entity -> {
        assertEquals(200, entity.getStatusCode().value());
        assertEquals("Juan", entity.getBody().getName());
      })
      .verifyComplete();
  }

  @Test
  void shouldDeleteStudent() {
    when(deleteUC.execute(1L)).thenReturn(Mono.empty());

    StepVerifier.create(controller.deleteStudent(1L))
      .assertNext(entity -> assertEquals(204, entity.getStatusCode().value()))
      .verifyComplete();

    verify(deleteUC).execute(1L);
  }
}