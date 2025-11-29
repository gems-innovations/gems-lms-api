package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTypeTest {

  @Test
  @DisplayName("Should throw when document type is null")
  void shouldThrowWhenNull() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> DocumentType.fromString(null)
    );

    assertEquals(StudentsConstants.DOCUMENT_TYPE_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when document type is empty")
  void shouldThrowWhenEmpty() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> DocumentType.fromString("")
    );

    assertEquals(StudentsConstants.DOCUMENT_TYPE_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when document type is only spaces")
  void shouldThrowWhenOnlySpaces() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> DocumentType.fromString("   ")
    );

    assertEquals(StudentsConstants.DOCUMENT_TYPE_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should return correct DocumentType for valid inputs")
  void shouldReturnCorrectType() {
    assertEquals(DocumentType.CC, DocumentType.fromString("cc"));
    assertEquals(DocumentType.CC, DocumentType.fromString(" CC "));
    assertEquals(DocumentType.TI, DocumentType.fromString("Ti"));
    assertEquals(DocumentType.CE, DocumentType.fromString(" ce "));
    assertEquals(DocumentType.PASSPORT, DocumentType.fromString("passport"));
    assertEquals(DocumentType.DNI, DocumentType.fromString("dni"));
  }

  @Test
  @DisplayName("Should throw when value does not match any DocumentType")
  void shouldThrowWhenInvalidType() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> DocumentType.fromString("ABC")
    );

    assertEquals(StudentsConstants.INVALID_DOCUMENT_TYPE, exception.getMessage());
  }

  @Test
  @DisplayName("toString should return enum name")
  void toStringShouldReturnEnumName() {
    assertEquals("CC", DocumentType.CC.toString());
    assertEquals("PASSPORT", DocumentType.PASSPORT.toString());
  }
}