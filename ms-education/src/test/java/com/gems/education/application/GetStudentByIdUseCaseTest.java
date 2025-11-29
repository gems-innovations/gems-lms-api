package com.gems.education.application;

import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.StudentResponse;
import com.gems.education.domain.entities.Student;
import com.gems.education.domain.constants.StudentsConstants;
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
@DisplayName("GetStudentByIdUseCase Tests")
class GetStudentByIdUseCaseTest {
  @Mock
  private StudentGateway studentGateway;

  private GetStudentByIdUseCase getStudentByIdUseCase;

  @BeforeEach
  void setUp() {
    getStudentByIdUseCase = new GetStudentByIdUseCase(studentGateway);
  }

  @Nested
  @DisplayName("When student exists")
  class WhenStudentExists {

    @Test
    @DisplayName("should return the student mapped to StudentResponse")
    void shouldReturnMappedStudent() {
      Long id = 1L;

      Student student = new Student(
        id,
        "John Doe",
        "john@test.com",
        LocalDate.of(2000, 1, 1),
        "CO",
        "Medellín",
        "CC",
        "123456789"
      );

      when(studentGateway.findById(any(StudentId.class)))
        .thenReturn(Mono.just(student));

      Mono<StudentResponse> result = getStudentByIdUseCase.execute(id);

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

      verify(studentGateway).findById(any(StudentId.class));
    }
  }

  @Nested
  @DisplayName("When student does NOT exist")
  class WhenStudentDoesNotExist {

    @Test
    @DisplayName("should throw StudentNotFoundException")
    void shouldThrowStudentNotFoundException() {
      Long id = 5L;

      when(studentGateway.findById(any(StudentId.class)))
        .thenReturn(Mono.empty());

      Mono<StudentResponse> result = getStudentByIdUseCase.execute(id);

      StepVerifier.create(result)
        .expectErrorMatches(error ->
          error instanceof StudentNotFoundException &&
            error.getMessage().contains(
              String.format(StudentsConstants.STUDENT_NOT_FOUND_MESSAGE, id)
            )
        )
        .verify();

      verify(studentGateway).findById(any(StudentId.class));
    }
  }
}