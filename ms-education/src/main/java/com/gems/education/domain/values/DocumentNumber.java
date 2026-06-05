package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;

import java.util.Objects;

public class DocumentNumber {
  private final String value;

  public DocumentNumber(String value) {

    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(StudentsConstants.DOCUMENT_NUMBER_CANNOT_BE_NULL_OR_EMPTY);
    }

    String trimmed = value.trim();

    if (trimmed.length() < StudentsConstants.DOCUMENT_NUMBER_MIN_LENGTH_VALUE) {
      throw new IllegalArgumentException(StudentsConstants.DOCUMENT_NUMBER_MIN_LENGTH);
    }

    if (trimmed.length() > StudentsConstants.DOCUMENT_NUMBER_MAX_LENGTH_VALUE) {
      throw new IllegalArgumentException(StudentsConstants.DOCUMENT_NUMBER_MAX_LENGTH);
    }

    if (!trimmed.matches(StudentsConstants.DOCUMENT_NUMBER_PATTERN_REGEX)) {
      throw new IllegalArgumentException(StudentsConstants.DOCUMENT_NUMBER_PATTERN);
    }

    this.value = trimmed;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DocumentNumber that = (DocumentNumber) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
