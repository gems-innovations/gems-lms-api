package com.gems.education.application.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StudentCommand Tests")
class StudentCommandTest {
  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create StudentCommand with valid parameters")
    void shouldCreateStudentCommandWithValidParameters() {
      String name = "John Doe";
      String email = "john@test.com";
      LocalDate birthDate = LocalDate.of(2000, 1, 1);
      String country = "CO";
      String city = "Medellín";
      String documentType = "CC";
      String documentNumber = "123456789";

      StudentCommand command = new StudentCommand(
        name, email, birthDate, country, city, documentType, documentNumber
      );

      assertNotNull(command);
      assertEquals(name, command.getName());
      assertEquals(email, command.getEmail());
      assertEquals(birthDate, command.getBirthDate());
      assertEquals(country, command.getCountry());
      assertEquals(city, command.getCity());
      assertEquals(documentType, command.getDocumentType());
      assertEquals(documentNumber, command.getDocumentNumber());
    }

    @Test
    @DisplayName("Should allow null values (no validation is performed)")
    void shouldAllowNullValues() {
      StudentCommand command = new StudentCommand(
        null, null, null, null, null, null, null
      );

      assertNull(command.getName());
      assertNull(command.getEmail());
      assertNull(command.getBirthDate());
      assertNull(command.getCountry());
      assertNull(command.getCity());
      assertNull(command.getDocumentType());
      assertNull(command.getDocumentNumber());
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("Should return correct name")
    void shouldReturnCorrectName() {
      StudentCommand command = new StudentCommand(
        "John Doe", "john@test.com", LocalDate.of(2000, 1, 1),
        "CO", "Medellín", "CC", "123456789"
      );

      assertEquals("John Doe", command.getName());
    }

    @Test
    @DisplayName("Should return correct email")
    void shouldReturnCorrectEmail() {
      StudentCommand command = new StudentCommand(
        "John Doe", "john@test.com", LocalDate.of(2000, 1, 1),
        "CO", "Medellín", "CC", "123456789"
      );

      assertEquals("john@test.com", command.getEmail());
    }

    @Test
    @DisplayName("Should return correct birth date")
    void shouldReturnCorrectBirthDate() {
      LocalDate date = LocalDate.of(2000, 1, 1);

      StudentCommand command = new StudentCommand(
        "John Doe", "john@test.com", date,
        "CO", "Medellín", "CC", "123456789"
      );

      assertEquals(date, command.getBirthDate());
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle very long name")
    void shouldHandleVeryLongName() {
      String longName = "a".repeat(200);

      StudentCommand command = new StudentCommand(
        longName, "john@test.com", LocalDate.of(2000, 1, 1),
        "CO", "Medellín", "CC", "123456789"
      );

      assertEquals(longName, command.getName());
    }

    @Test
    @DisplayName("Should handle special characters in email")
    void shouldHandleSpecialCharactersInEmail() {
      String email = "john+tag@example-domain.com";

      StudentCommand command = new StudentCommand(
        "John Doe", email, LocalDate.of(2000, 1, 1),
        "CO", "Medellín", "CC", "123456789"
      );

      assertEquals(email, command.getEmail());
    }

    @Test
    @DisplayName("Should handle special characters in city")
    void shouldHandleSpecialCharactersInCity() {
      String city = "São Paulo";

      StudentCommand command = new StudentCommand(
        "John Doe", "john@test.com", LocalDate.of(2000, 1, 1),
        "BR", city, "CC", "123456789"
      );

      assertEquals(city, command.getCity());
    }
  }
}