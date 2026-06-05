package com.gems.education.application;

import com.gems.education.application.command.StudentCommand;
import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.StudentResponse;
import com.gems.education.domain.constants.StudentsConstants;
import com.gems.education.domain.entities.Student;
import com.gems.education.infrastructure.driving.rest.exeption.StudentAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterStudentUseCase Tests")
class RegisterStudentUseCaseTest {
  @Mock
  private StudentGateway studentGateway;

  private RegisterStudentUseCase registerStudentUseCase;

  @BeforeEach
  void setUp() {
    registerStudentUseCase = new RegisterStudentUseCase(studentGateway);
  }

  @Nested
  @DisplayName("When email already exists")
  class WhenEmailExists {

    @Test
    @DisplayName("should throw StudentAlreadyExistsException due to email")
    void shouldThrowErrorWhenEmailExists() {
      StudentCommand command = new StudentCommand(
        "John Doe",
        "john@test.com",
        LocalDate.of(2000, 1, 1),
        "CO",
        "Medellín",
        "CC",
        "123456789"
      );

      when(studentGateway.existsByEmail(any()))
        .thenReturn(Mono.just(true));

      Mono<StudentResponse> result = registerStudentUseCase.execute(command);

      StepVerifier.create(result)
        .expectErrorMatches(error ->
          error instanceof StudentAlreadyExistsException &&
            error.getMessage().contains(
              String.format(StudentsConstants.STUDENT_ALREADY_EXISTS_EMAIL_MESSAGE, command.getEmail())
            )
        )
        .verify();

      verify(studentGateway).existsByEmail(any());
      verify(studentGateway, never()).existsByDocumentNumber(any());
      verify(studentGateway, never()).save(any());
    }
  }

  @Nested
  @DisplayName("When document number already exists")
  class WhenDocumentNumberExists {

    @Test
    @DisplayName("should throw StudentAlreadyExistsException due to document number")
    void shouldThrowErrorWhenDocumentExists() {
      StudentCommand command = new StudentCommand(
        "John Doe",
        "john@test.com",
        LocalDate.of(2000, 1, 1),
        "CO",
        "Medellín",
        "CC",
        "123456789"
      );

      when(studentGateway.existsByEmail(any()))
        .thenReturn(Mono.just(false));

      when(studentGateway.existsByDocumentNumber(any()))
        .thenReturn(Mono.just(true));

      Mono<StudentResponse> result = registerStudentUseCase.execute(command);

      StepVerifier.create(result)
        .expectErrorMatches(error ->
          error instanceof StudentAlreadyExistsException &&
            error.getMessage().contains(
              String.format(StudentsConstants.STUDENT_ALREADY_EXISTS_DOCUMENT_MESSAGE, command.getDocumentNumber())
            )
        )
        .verify();

      verify(studentGateway).existsByEmail(any());
      verify(studentGateway).existsByDocumentNumber(any());
      verify(studentGateway, never()).save(any());
    }
  }

  @Nested
  @DisplayName("When student does not exist by email or document number")
  class WhenStudentDoesNotExist {

    @Test
    @DisplayName("should register student successfully")
    void shouldRegisterStudentSuccessfully() {
      StudentCommand command = new StudentCommand(
        "John Doe",
        "john@test.com",
        LocalDate.of(2000, 1, 1),
        "CO",
        "Medellín",
        "CC",
        "123456789"
      );

      Student savedStudent = new Student(
        1L,
        "John Doe",
        "john@test.com",
        LocalDate.of(2000, 1, 1),
        "CO",
        "Medellín",
        "CC",
        "123456789"
      );

      when(studentGateway.existsByEmail(any()))
        .thenReturn(Mono.just(false));

      when(studentGateway.existsByDocumentNumber(any()))
        .thenReturn(Mono.just(false));

      when(studentGateway.save(any(Student.class)))
        .thenReturn(Mono.just(savedStudent));

      Mono<StudentResponse> result = registerStudentUseCase.execute(command);

      StepVerifier.create(result)
        .assertNext(response -> {
          org.junit.jupiter.api.Assertions.assertEquals(1L, response.getId());
          org.junit.jupiter.api.Assertions.assertEquals("John Doe", response.getName());
          org.junit.jupiter.api.Assertions.assertEquals("john@test.com", response.getEmail());
          org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(2000, 1, 1), response.getBirthDate());
          org.junit.jupiter.api.Assertions.assertEquals("CO", response.getCountry());
          org.junit.jupiter.api.Assertions.assertEquals("Medellín", response.getCity());
          org.junit.jupiter.api.Assertions.assertEquals("CC", response.getDocumentType());
          org.junit.jupiter.api.Assertions.assertEquals("123456789", response.getDocumentNumber());
        })
        .verifyComplete();

      verify(studentGateway).existsByEmail(any());
      verify(studentGateway).existsByDocumentNumber(any());
      verify(studentGateway).save(any(Student.class));
    }
  }
}