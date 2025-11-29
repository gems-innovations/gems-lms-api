package com.gems.education.domain.entities;

import com.gems.education.domain.values.DocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


@DisplayName("Student Entity Tests")
class StudentTest {

  @Test
  @DisplayName("Should create a student without id")
  void shouldCreateStudentWithoutId() {
    Student student = new Student(
      "Juan",
      "juan@example.com",
      LocalDate.of(2000, 1, 1),
      "Colombia",
      "Medellín",
      "CC",
      "12345"
    );

    assertNull(student.getId());
    assertEquals("Juan", student.getName().getValue());
    assertEquals("juan@example.com", student.getEmail().getValue());
    assertEquals(LocalDate.of(2000, 1, 1), student.getBirthDate().getValue());
    assertEquals("Colombia", student.getCountry().getValue());
    assertEquals("Medellín", student.getCity().getValue());
    assertEquals(DocumentType.CC, student.getDocumentType());
    assertEquals("12345", student.getDocumentNumber().getValue());
  }

  @Test
  @DisplayName("Should update email and keep other fields")
  void shouldUpdateEmail() {
    Student student = new Student(
      1L,
      "Juan",
      "old@mail.com",
      LocalDate.of(2000, 1, 1),
      "Colombia",
      "Bogotá",
      "CC",
      "9999999"
    );

    Student updated = student.updateEmail("new@mail.com");

    assertEquals(1L, updated.getId().getValue());
    assertEquals("new@mail.com", updated.getEmail().getValue());

    // unchanged values
    assertEquals("Juan", updated.getName().getValue());
    assertEquals("Colombia", updated.getCountry().getValue());
    assertEquals("Bogotá", updated.getCity().getValue());
    assertEquals("9999999", updated.getDocumentNumber().getValue());
    assertEquals(DocumentType.CC, updated.getDocumentType());
  }

  @Test
  @DisplayName("Should update city correctly")
  void shouldUpdateCity() {
    Student student = new Student(
      1L,
      "Ana",
      "ana@mail.com",
      LocalDate.of(1999, 5, 10),
      "Colombia",
      "Cali",
      "TI",
      "123456"
    );

    Student updated = student.updateCity("Medellín");

    assertEquals("Medellín", updated.getCity().getValue());
    assertEquals("Cali", student.getCity().getValue());
  }

  @Test
  @DisplayName("Should update country correctly")
  void shouldUpdateCountry() {
    Student student = new Student(
      1L,
      "Ana",
      "ana@mail.com",
      LocalDate.of(1999, 5, 10),
      "Colombia",
      "Cali",
      "TI",
      "123456"
    );

    Student updated = student.updateCountry("México");

    assertEquals("México", updated.getCountry().getValue());
    assertEquals("Colombia", student.getCountry().getValue());
  }
}