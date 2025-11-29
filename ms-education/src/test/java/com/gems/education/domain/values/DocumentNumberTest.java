package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class DocumentNumberTest {

  @Test
  @DisplayName("Should throw when document number is null")
  void shouldThrowWhenNull() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new DocumentNumber(null)
    );

    assertEquals(StudentsConstants.DOCUMENT_NUMBER_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when document number is empty")
  void shouldThrowWhenEmpty() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new DocumentNumber("")
    );

    assertEquals(StudentsConstants.DOCUMENT_NUMBER_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when document number contains only spaces")
  void shouldThrowWhenOnlySpaces() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new DocumentNumber("   ")
    );

    assertEquals(StudentsConstants.DOCUMENT_NUMBER_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when document number is shorter than minimum length")
  void shouldThrowWhenTooShort() {
    int min = StudentsConstants.DOCUMENT_NUMBER_MIN_LENGTH_VALUE;
    String shorter = "1".repeat(min - 1);

    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new DocumentNumber(shorter)
    );

    assertEquals(StudentsConstants.DOCUMENT_NUMBER_MIN_LENGTH, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when document number is longer than maximum length")
  void shouldThrowWhenTooLong() {
    int max = StudentsConstants.DOCUMENT_NUMBER_MAX_LENGTH_VALUE;
    String longer = "1".repeat(max + 1);

    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new DocumentNumber(longer)
    );

    assertEquals(StudentsConstants.DOCUMENT_NUMBER_MAX_LENGTH, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when document number does not match regex pattern")
  void shouldThrowWhenPatternInvalid() {
    String invalid = "ABC 123"; // espacio → rompe el regex ^[a-zA-Z0-9-]+$

    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new DocumentNumber(invalid)
    );

    assertEquals(StudentsConstants.DOCUMENT_NUMBER_PATTERN, exception.getMessage());
  }

  @Test
  @DisplayName("Should create valid DocumentNumber and apply trim")
  void shouldCreateValidDocumentNumber() {
    String input = "   12345678   ";
    DocumentNumber dn = new DocumentNumber(input);

    assertEquals("12345678", dn.getValue());
  }

  @Test
  @DisplayName("Equals should return true for equal values")
  void equalsShouldBeTrue() {
    DocumentNumber d1 = new DocumentNumber("123456");
    DocumentNumber d2 = new DocumentNumber("123456");

    assertEquals(d1, d2);
    assertEquals(d1.hashCode(), d2.hashCode());
  }

  @Test
  @DisplayName("Equals should return false for different values")
  void equalsShouldBeFalse() {
    DocumentNumber d1 = new DocumentNumber("123456");
    DocumentNumber d2 = new DocumentNumber("789012");

    assertNotEquals(d1, d2);
  }

  @Test
  @DisplayName("toString should return document number")
  void toStringShouldReturnValue() {
    DocumentNumber dn = new DocumentNumber("123456");

    assertEquals("123456", dn.toString());
  }
}