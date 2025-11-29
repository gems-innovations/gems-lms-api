package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BirthDateTest {

  @Test
  @DisplayName("Should throw exception when birthdate is null")
  void shouldThrowWhenBirthdateIsNull() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new BirthDate(null)
    );

    assertEquals(StudentsConstants.BIRTHDATE_CANNOT_BE_NULL, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw exception when birthdate is in the future")
  void shouldThrowWhenBirthdateIsInFuture() {
    LocalDate futureDate = LocalDate.now().plusDays(1);

    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new BirthDate(futureDate)
    );

    assertEquals(StudentsConstants.BIRTHDATE_CANNOT_BE_IN_THE_FUTURE, exception.getMessage());
  }

  @Test
  @DisplayName("Should create BirthDate successfully when date is valid")
  void shouldCreateValidBirthdate() {
    LocalDate date = LocalDate.of(2000, 1, 1);
    BirthDate birthDate = new BirthDate(date);

    assertEquals(date, birthDate.getValue());
  }

  @Test
  @DisplayName("Equals should return true for same date")
  void equalsShouldWorkForSameValue() {
    BirthDate b1 = new BirthDate(LocalDate.of(2000, 1, 1));
    BirthDate b2 = new BirthDate(LocalDate.of(2000, 1, 1));

    assertEquals(b1, b2);
    assertEquals(b1.hashCode(), b2.hashCode());
  }

  @Test
  @DisplayName("Equals should return false for different dates")
  void equalsShouldFailForDifferentValue() {
    BirthDate b1 = new BirthDate(LocalDate.of(2000, 1, 1));
    BirthDate b2 = new BirthDate(LocalDate.of(1990, 5, 20));

    assertNotEquals(b1, b2);
  }

  @Test
  @DisplayName("toString should return ISO date format")
  void toStringShouldReturnValue() {
    LocalDate date = LocalDate.of(2000, 1, 1);
    BirthDate birthDate = new BirthDate(date);

    assertEquals("2000-01-01", birthDate.toString());
  }
}