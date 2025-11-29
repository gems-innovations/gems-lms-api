package com.gems.education.infrastructure.driven.postgresql;


import com.gems.education.domain.entities.Student;
import com.gems.education.domain.values.DocumentNumber;
import com.gems.education.domain.values.Email;
import com.gems.education.domain.values.StudentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("StudentRepositoryAdapter Tests")
class StudentRepositoryAdapterTest {

  private IStudentRepository studentRepository;
  private StudentRepositoryAdapter adapter;

  @BeforeEach
  void setup() {
    studentRepository = mock(IStudentRepository.class);
    adapter = new StudentRepositoryAdapter(studentRepository);
  }

  @Nested
  @DisplayName("save() Tests")
  class SaveTests {

    @Test
    @DisplayName("Should save student correctly and map result to domain")
    void shouldSaveStudent() {

      Student domainStudent = new Student(
        "Juan",
        "juan@example.com",
        LocalDate.of(2000, 1, 1),
        "Colombia",
        "Medellín",
        "CC",
        "123456"
      );

      StudentEntity savedEntity = new StudentEntity(
        1L,
        "Juan",
        "juan@example.com",
        LocalDate.of(2000, 1, 1),
        "Colombia",
        "Medellín",
        "CC",
        "123456"
      );

      when(studentRepository.save(any(StudentEntity.class)))
        .thenReturn(Mono.just(savedEntity));

      StepVerifier.create(adapter.save(domainStudent))
        .assertNext(result -> {
          assertEquals(1L, result.getId().getValue());
          assertEquals("Juan", result.getName().getValue());
          assertEquals("juan@example.com", result.getEmail().getValue());
        })
        .verifyComplete();

      ArgumentCaptor<StudentEntity> captor = ArgumentCaptor.forClass(StudentEntity.class);
      verify(studentRepository).save(captor.capture());
      StudentEntity entitySent = captor.getValue();

      assertNull(entitySent.getId());
      assertEquals("Juan", entitySent.getName());
      assertEquals("juan@example.com", entitySent.getEmail());
    }
  }

  @Nested
  @DisplayName("findById() Tests")
  class FindByIdTests {

    @Test
    @DisplayName("Should return domain student when found")
    void shouldReturnStudentWhenFound() {
      StudentEntity entity = new StudentEntity(
        10L, "Ana", "ana@example.com",
        LocalDate.of(1999, 5, 10),
        "Colombia", "Cali",
        "TI", "123456789"
      );

      when(studentRepository.findById(10L))
        .thenReturn(Mono.just(entity));

      StepVerifier.create(adapter.findById(new StudentId(10L)))
        .assertNext(student -> {
          assertEquals(10L, student.getId().getValue());
          assertEquals("Ana", student.getName().getValue());
        })
        .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty Mono when student not found")
    void shouldReturnEmptyWhenNotFound() {
      when(studentRepository.findById(10L))
        .thenReturn(Mono.empty());

      StepVerifier.create(adapter.findById(new StudentId(10L)))
        .verifyComplete();
    }
  }

  @Nested
  @DisplayName("findByEmail() Tests")
  class FindByEmailTests {

    @Test
    @DisplayName("Should return student when email exists")
    void shouldReturnStudentByEmail() {
      StudentEntity entity = new StudentEntity(
        5L, "Laura", "laura@example.com",
        LocalDate.of(1998, 2, 20),
        "Colombia", "Bogotá",
        "CC", "654321"
      );

      when(studentRepository.findByEmail("laura@example.com"))
        .thenReturn(Mono.just(entity));

      StepVerifier.create(adapter.findByEmail(new Email("laura@example.com")))
        .assertNext(st -> assertEquals("Laura", st.getName().getValue()))
        .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty Mono when email not found")
    void shouldReturnEmptyForMissingEmail() {
      when(studentRepository.findByEmail("none@example.com"))
        .thenReturn(Mono.empty());

      StepVerifier.create(adapter.findByEmail(new Email("none@example.com")))
        .verifyComplete();
    }
  }

  @Test
  @DisplayName("existsByEmail() should return repository result")
  void shouldReturnExistsByEmail() {
    when(studentRepository.existsByEmail("test@mail.com"))
      .thenReturn(Mono.just(true));

    StepVerifier.create(adapter.existsByEmail(new Email("test@mail.com")))
      .expectNext(true)
      .verifyComplete();
  }

  @Test
  @DisplayName("existsByDocumentNumber() should return repository result")
  void shouldReturnExistsByDocument() {
    when(studentRepository.existsByDocumentNumber("123456"))
      .thenReturn(Mono.just(false));

    StepVerifier.create(adapter.existsByDocumentNumber(new DocumentNumber("123456")))
      .expectNext(false)
      .verifyComplete();
  }

  @Test
  @DisplayName("deleteById() should call repository")
  void shouldDeleteById() {
    when(studentRepository.deleteById(7L))
      .thenReturn(Mono.empty());

    StepVerifier.create(adapter.deleteById(new StudentId(7L)))
      .verifyComplete();

    verify(studentRepository).deleteById(7L);
  }

  @Test
  @DisplayName("findAll() should map repository entities to domain")
  void shouldFindAll() {
    StudentEntity e1 = new StudentEntity(
      1L, "Juan", "juan@example.com",
      LocalDate.of(2000, 1, 1),
      "Colombia", "Medellín", "CC", "11111"
    );

    StudentEntity e2 = new StudentEntity(
      2L, "Ana", "ana@example.com",
      LocalDate.of(1999, 5, 10),
      "Colombia", "Cali", "TI", "22222"
    );

    when(studentRepository.findAll())
      .thenReturn(Flux.just(e1, e2));

    StepVerifier.create(adapter.findAll())
      .assertNext(st -> assertEquals("Juan", st.getName().getValue()))
      .assertNext(st -> assertEquals("Ana", st.getName().getValue()))
      .verifyComplete();
  }
}