package com.gems.education.application.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StudentResponse Tests")
class StudentResponseTest {
  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create StudentResponse with valid parameters")
    void shouldCreateStudentResponseWithValidParameters() {
      Long id = 1L;
      String name = "John Doe";
      String email = "john@test.com";
      LocalDate birthDate = LocalDate.of(2000, 1, 1);
      String country = "CO";
      String city = "Medellín";
      String documentType = "CC";
      String documentNumber = "123456789";

      StudentResponse response = new StudentResponse(
        id, name, email, birthDate, country, city, documentType, documentNumber
      );

      assertNotNull(response);
      assertEquals(id, response.getId());
      assertEquals(name, response.getName());
      assertEquals(email, response.getEmail());
      assertEquals(birthDate, response.getBirthDate());
      assertEquals(country, response.getCountry());
      assertEquals(city, response.getCity());
      assertEquals(documentType, response.getDocumentType());
      assertEquals(documentNumber, response.getDocumentNumber());
    }

    @Test
    @DisplayName("Should allow null values (no validation is performed)")
    void shouldAllowNullValues() {
      StudentResponse response = new StudentResponse(
        null, null, null, null, null, null, null, null
      );

      assertNull(response.getId());
      assertNull(response.getName());
      assertNull(response.getEmail());
      assertNull(response.getBirthDate());
      assertNull(response.getCountry());
      assertNull(response.getCity());
      assertNull(response.getDocumentType());
      assertNull(response.getDocumentNumber());
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("Should return correct name")
    void shouldReturnCorrectName() {
      StudentResponse response = new StudentResponse(
        1L, "John Doe", "john@test.com",
        LocalDate.of(2000, 1, 1),
        "CO", "Medellín",
        "CC", "123456789"
      );

      assertEquals("John Doe", response.getName());
    }

    @Test
    @DisplayName("Should return correct email")
    void shouldReturnCorrectEmail() {
      StudentResponse response = new StudentResponse(
        1L, "John Doe", "john@test.com",
        LocalDate.of(2000, 1, 1),
        "CO", "Medellín",
        "CC", "123456789"
      );

      assertEquals("john@test.com", response.getEmail());
    }

    @Test
    @DisplayName("Should return correct birth date")
    void shouldReturnCorrectBirthDate() {
      LocalDate date = LocalDate.of(2000, 1, 1);

      StudentResponse response = new StudentResponse(
        1L, "John Doe", "john@test.com",
        date,
        "CO", "Medellín",
        "CC", "123456789"
      );

      assertEquals(date, response.getBirthDate());
    }
  }

  @Nested
  @DisplayName("Edge Cases Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle long strings")
    void shouldHandleLongStrings() {
      String longName = "a".repeat(300);

      StudentResponse response = new StudentResponse(
        1L, longName, "john@test.com",
        LocalDate.of(2000, 1, 1),
        "CO", "Medellín",
        "CC", "123456789"
      );

      assertEquals(longName, response.getName());
    }

    @Test
    @DisplayName("Should handle special characters in fields")
    void shouldHandleSpecialCharacters() {
      StudentResponse response = new StudentResponse(
        1L, "Jöhn Dœ", "john+tag@test-domain.com",
        LocalDate.of(2000, 1, 1),
        "CÖ", "São Paulo",
        "TI", "ABC-123-!@#"
      );

      assertEquals("Jöhn Dœ", response.getName());
      assertEquals("john+tag@test-domain.com", response.getEmail());
      assertEquals("CÖ", response.getCountry());
      assertEquals("São Paulo", response.getCity());
      assertEquals("TI", response.getDocumentType());
      assertEquals("ABC-123-!@#", response.getDocumentNumber());
    }
  }
}