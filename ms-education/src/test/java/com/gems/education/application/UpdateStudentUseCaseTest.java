package com.gems.education.application;

import com.gems.education.application.command.StudentCommand;
import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.StudentResponse;
import com.gems.education.domain.constants.StudentsConstants;
import com.gems.education.domain.entities.Student;
import com.gems.education.domain.values.StudentId;
import com.gems.education.infrastructure.driving.rest.exeption.StudentNotFoundException;
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
@DisplayName("UpdateStudentUseCase Tests")
class UpdateStudentUseCaseTest {
  @Mock
  private StudentGateway studentGateway;

  private UpdateStudentUseCase updateStudentUseCase;

  @BeforeEach
  void setUp() {
    updateStudentUseCase = new UpdateStudentUseCase(studentGateway);
  }

  @Nested
  @DisplayName("When student does NOT exist")
  class WhenStudentDoesNotExist {

    @Test
    @DisplayName("should throw StudentNotFoundException")
    void shouldThrowNotFound() {
      Long id = 5L;

      StudentCommand command = new StudentCommand(
        "John Updated",
        "updated@test.com",
        LocalDate.of(1999, 2, 2),
        "US",
        "Miami",
        "CC",
        "999999999"
      );

      when(studentGateway.findById(any(StudentId.class)))
        .thenReturn(Mono.empty());

      Mono<StudentResponse> result = updateStudentUseCase.execute(id, command);

      StepVerifier.create(result)
        .expectErrorMatches(error ->
          error instanceof StudentNotFoundException &&
            error.getMessage().contains(
              String.format(StudentsConstants.STUDENT_NOT_FOUND_MESSAGE, id)
            )
        )
        .verify();

      verify(studentGateway).findById(any(StudentId.class));
      verify(studentGateway, never()).save(any());
    }
  }

  @Nested
  @DisplayName("When student exists")
  class WhenStudentExists {

    @Test
    @DisplayName("should update student successfully")
    void shouldUpdateStudentSuccessfully() {
      Long id = 1L;

      Student existing = new Student(
        id,
        "Old Name",
        "old@test.com",
        LocalDate.of(2000, 1, 1),
        "CO",
        "Medellín",
        "CC",
        "111111111"
      );

      StudentCommand command = new StudentCommand(
        "New Name",
        "new@test.com",
        LocalDate.of(1998, 12, 12),
        "US",
        "New York",
        "TI",
        "222222222"
      );

      Student updated = new Student(
        id,
        "New Name",
        "new@test.com",
        LocalDate.of(1998, 12, 12),
        "US",
        "New York",
        "TI",
        "222222222"
      );

      when(studentGateway.findById(any(StudentId.class)))
        .thenReturn(Mono.just(existing));

      when(studentGateway.save(any(Student.class)))
        .thenReturn(Mono.just(updated));

      Mono<StudentResponse> result = updateStudentUseCase.execute(id, command);

      StepVerifier.create(result)
        .assertNext(response -> {
          org.junit.jupiter.api.Assertions.assertEquals(1L, response.getId());
          org.junit.jupiter.api.Assertions.assertEquals("New Name", response.getName());
          org.junit.jupiter.api.Assertions.assertEquals("new@test.com", response.getEmail());
          org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(1998, 12, 12), response.getBirthDate());
          org.junit.jupiter.api.Assertions.assertEquals("US", response.getCountry());
          org.junit.jupiter.api.Assertions.assertEquals("New York", response.getCity());
          org.junit.jupiter.api.Assertions.assertEquals("TI", response.getDocumentType());
          org.junit.jupiter.api.Assertions.assertEquals("222222222", response.getDocumentNumber());
        })
        .verifyComplete();

      verify(studentGateway).findById(any(StudentId.class));
      verify(studentGateway).save(any(Student.class));
    }
  }
}