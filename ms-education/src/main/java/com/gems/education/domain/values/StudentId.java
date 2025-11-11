package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;

import java.util.Objects;

public class StudentId {

  private final Long value;

  public StudentId(Long value) {
    if (value == null) {
      throw new IllegalArgumentException(StudentsConstants.STUDENT_ID_CANNOT_BE_NULL_OR_EMPTY);
    }
    this.value = value;
  }

  public static StudentId generate() {
    return null;
  }

  public Long getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StudentId userId = (StudentId) o;
    return Objects.equals(value, userId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
