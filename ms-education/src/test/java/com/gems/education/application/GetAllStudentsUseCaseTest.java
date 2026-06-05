package com.gems.education.application;


import com.gems.education.application.gateway.StudentGateway;
import com.gems.education.application.response.StudentResponse;
import com.gems.education.domain.entities.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAllStudentsUseCase Tests")
class GetAllStudentsUseCaseTest {
  @Mock
  private StudentGateway studentGateway;

  private GetAllStudentsUseCase getAllStudentsUseCase;

  @BeforeEach
  void setUp() {
    getAllStudentsUseCase = new GetAllStudentsUseCase(studentGateway);
  }

  @Nested
  @DisplayName("When students exist")
  class WhenStudentsExist {

    @Test
    @DisplayName("should return all students mapped to StudentResponse")
    void shouldReturnAllStudentsMapped() {
      Student student1 = new Student(
        1L,
        "John Doe",
        "john@test.com",
        LocalDate.of(2000, 1, 1),
        "CO",
        "Medellín",
        "CC",
        "123456789"
      );

      Student student2 = new Student(
        2L,
        "Jane Smith",
        "jane@test.com",
        LocalDate.of(1995, 5, 10),
        "CO",
        "Bogotá",
        "TI",
        "987654321"
      );

      when(studentGateway.findAll())
        .thenReturn(Flux.just(student1, student2));

      Flux<StudentResponse> result = getAllStudentsUseCase.execute();

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
        .assertNext(response -> {
          org.junit.jupiter.api.Assertions.assertEquals(2L, response.getId());
          org.junit.jupiter.api.Assertions.assertEquals("Jane Smith", response.getName());
          org.junit.jupiter.api.Assertions.assertEquals("jane@test.com", response.getEmail());
          org.junit.jupiter.api.Assertions.assertEquals(LocalDate.of(1995, 5, 10), response.getBirthDate());
          org.junit.jupiter.api.Assertions.assertEquals("CO", response.getCountry());
          org.junit.jupiter.api.Assertions.assertEquals("Bogotá", response.getCity());
          org.junit.jupiter.api.Assertions.assertEquals("TI", response.getDocumentType());
          org.junit.jupiter.api.Assertions.assertEquals("987654321", response.getDocumentNumber());
        })
        .verifyComplete();

      verify(studentGateway).findAll();
    }
  }

  @Nested
  @DisplayName("When no students exist")
  class WhenNoStudentsExist {

    @Test
    @DisplayName("should return an empty Flux")
    void shouldReturnEmptyFlux() {
      when(studentGateway.findAll()).thenReturn(Flux.empty());

      Flux<StudentResponse> result = getAllStudentsUseCase.execute();

      StepVerifier.create(result)
        .verifyComplete();

      verify(studentGateway).findAll();
    }
  }
}