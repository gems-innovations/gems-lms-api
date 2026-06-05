package com.gems.education.infrastructure.driven.postgresql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StudentEntity Tests")
class StudentEntityTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should create StudentEntity with all fields using full constructor")
    void shouldCreateWithFullConstructor() {
      Long id = 1L;
      String name = "John Doe";
      String email = "john@example.com";
      LocalDate birthDate = LocalDate.of(2000, 1, 1);
      String country = "Colombia";
      String city = "Medellín";
      String documentType = "CC";
      String documentNumber = "123456789";

      StudentEntity entity = new StudentEntity(
        id,
        name,
        email,
        birthDate,
        country,
        city,
        documentType,
        documentNumber
      );

      assertEquals(id, entity.getId());
      assertEquals(name, entity.getName());
      assertEquals(email, entity.getEmail());
      assertEquals(birthDate, entity.getBirthDate());
      assertEquals(country, entity.getCountry());
      assertEquals(city, entity.getCity());
      assertEquals(documentType, entity.getDocumentType());
      assertEquals(documentNumber, entity.getDocumentNumber());
    }

    @Test
    @DisplayName("Should allow creation with no-args constructor and setters")
    void shouldCreateWithNoArgsConstructorAndSetters() {
      StudentEntity entity = new StudentEntity();

      Long id = 2L;
      String name = "Ana Gómez";
      String email = "ana@example.com";
      LocalDate birthDate = LocalDate.of(1999, 5, 10);
      String country = "México";
      String city = "CDMX";
      String documentType = "DNI";
      String documentNumber = "987654321";

      entity.setId(id);
      entity.setName(name);
      entity.setEmail(email);
      entity.setBirthDate(birthDate);
      entity.setCountry(country);
      entity.setCity(city);
      entity.setDocumentType(documentType);
      entity.setDocumentNumber(documentNumber);

      assertEquals(id, entity.getId());
      assertEquals(name, entity.getName());
      assertEquals(email, entity.getEmail());
      assertEquals(birthDate, entity.getBirthDate());
      assertEquals(country, entity.getCountry());
      assertEquals(city, entity.getCity());
      assertEquals(documentType, entity.getDocumentType());
      assertEquals(documentNumber, entity.getDocumentNumber());
    }
  }

  @Nested
  @DisplayName("Getter & Setter Tests")
  class GetterSetterTests {

    @Test
    @DisplayName("Should update and retrieve fields correctly")
    void shouldUpdateFields() {
      StudentEntity entity = new StudentEntity();

      entity.setId(10L);
      entity.setName("Carlos López");
      entity.setEmail("carlos@example.com");
      entity.setBirthDate(LocalDate.of(1995, 3, 15));
      entity.setCountry("Perú");
      entity.setCity("Lima");
      entity.setDocumentType("CE");
      entity.setDocumentNumber("ABC-123-XYZ");

      assertAll(
        () -> assertEquals(10L, entity.getId()),
        () -> assertEquals("Carlos López", entity.getName()),
        () -> assertEquals("carlos@example.com", entity.getEmail()),
        () -> assertEquals(LocalDate.of(1995, 3, 15), entity.getBirthDate()),
        () -> assertEquals("Perú", entity.getCountry()),
        () -> assertEquals("Lima", entity.getCity()),
        () -> assertEquals("CE", entity.getDocumentType()),
        () -> assertEquals("ABC-123-XYZ", entity.getDocumentNumber())
      );
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("Should return formatted string with key fields")
    void shouldReturnFormattedToString() {
      StudentEntity entity = new StudentEntity(
        1L,
        "John Doe",
        "john@example.com",
        LocalDate.of(2000, 1, 1),
        "Colombia",
        "Medellín",
        "CC",
        "123456789"
      );

      String result = entity.toString();

      assertTrue(result.contains("StudentEntity["));
      assertTrue(result.contains("id=1"));
      assertTrue(result.contains("name=John Doe"));
      assertTrue(result.contains("email=john@example.com"));
      assertTrue(result.contains("documentType=CC"));
      assertTrue(result.contains("documentNumber=123456789"));
      assertTrue(result.contains("city=Medellín"));
      assertTrue(result.contains("country=Colombia"));
    }
  }
}