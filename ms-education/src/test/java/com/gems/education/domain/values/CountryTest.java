package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class CountryTest {

  @Test
  @DisplayName("Should throw when country is null")
  void shouldThrowWhenCountryIsNull() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new Country(null)
    );

    assertEquals(StudentsConstants.COUNTRY_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when country is empty")
  void shouldThrowWhenCountryIsEmpty() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new Country("")
    );

    assertEquals(StudentsConstants.COUNTRY_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when country is only spaces")
  void shouldThrowWhenCountryIsOnlySpaces() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new Country("   ")
    );

    assertEquals(StudentsConstants.COUNTRY_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when country has less than 2 characters")
  void shouldThrowWhenCountryIsTooShort() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new Country("A")
    );

    assertEquals(StudentsConstants.COUNTRY_MIN_LENGTH, exception.getMessage());
  }

  @Test
  @DisplayName("Should create a valid country and trim spaces")
  void shouldCreateValidCountry() {
    Country country = new Country("  Colombia  ");

    assertEquals("Colombia", country.getValue());
  }

  @Test
  @DisplayName("Equals should return true for equal values")
  void equalsShouldBeTrue() {
    Country c1 = new Country("Colombia");
    Country c2 = new Country("Colombia");

    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());
  }

  @Test
  @DisplayName("Equals should return false for different values")
  void equalsShouldBeFalse() {
    Country c1 = new Country("Colombia");
    Country c2 = new Country("Argentina");

    assertNotEquals(c1, c2);
  }

  @Test
  @DisplayName("toString should return the country value")
  void toStringShouldReturnValue() {
    Country country = new Country("México");

    assertEquals("México", country.toString());
  }
}