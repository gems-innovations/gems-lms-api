package com.gems.education.application;

import com.gems.education.application.gateway.StudentGateway;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteStudentUseCase Tests")
class DeleteStudentUseCaseTest {
  @Mock
  private StudentGateway studentGateway;

  private DeleteStudentUseCase deleteStudentUseCase;

  @BeforeEach
  void setUp() {
    deleteStudentUseCase = new DeleteStudentUseCase(studentGateway);
  }

  @Nested
  @DisplayName("When student exists")
  class WhenStudentExists {

    @Test
    @DisplayName("should delete the student successfully")
    void shouldDeleteStudentSuccessfully() {
      Long id = 1L;
      Student student = mock(Student.class);

      when(studentGateway.findById(any(StudentId.class)))
        .thenReturn(Mono.just(student));

      when(studentGateway.deleteById(any(StudentId.class)))
        .thenReturn(Mono.empty());

      // Act
      Mono<Void> result = deleteStudentUseCase.execute(id);

      // Assert
      StepVerifier.create(result)
        .verifyComplete();

      verify(studentGateway).findById(any(StudentId.class));
      verify(studentGateway).deleteById(any(StudentId.class));
    }
  }

  @Nested
  @DisplayName("When student does NOT exist")
  class WhenStudentDoesNotExist {

    @Test
    @DisplayName("should throw StudentNotFoundException")
    void shouldThrowStudentNotFoundException() {
      // Arrange
      Long id = 5L;

      when(studentGateway.findById(any(StudentId.class)))
        .thenReturn(Mono.empty());

      // Act
      Mono<Void> result = deleteStudentUseCase.execute(id);

      // Assert
      StepVerifier.create(result)
        .expectErrorMatches(error ->
          error instanceof StudentNotFoundException &&
            error.getMessage().contains(
              String.format(StudentsConstants.STUDENT_NOT_FOUND_MESSAGE, id)
            )
        )
        .verify();

      verify(studentGateway).findById(any(StudentId.class));
      verify(studentGateway, never()).deleteById(any(StudentId.class));
    }
  }
}