package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityTest {

  @Test
  @DisplayName("Should throw when city is null")
  void shouldThrowWhenCityIsNull() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new City(null)
    );

    assertEquals(StudentsConstants.CITY_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when city is empty")
  void shouldThrowWhenCityIsEmpty() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new City("")
    );

    assertEquals(StudentsConstants.CITY_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when city contains only spaces")
  void shouldThrowWhenCityIsOnlySpaces() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new City("   ")
    );

    assertEquals(StudentsConstants.CITY_CANNOT_BE_NULL_OR_EMPTY, exception.getMessage());
  }

  @Test
  @DisplayName("Should throw when city length is less than 2")
  void shouldThrowWhenCityIsTooShort() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new City("A")
    );

    assertEquals(StudentsConstants.CITY_MIN_LENGTH, exception.getMessage());
  }

  @Test
  @DisplayName("Should create valid City and trim spaces")
  void shouldCreateValidCity() {
    City city = new City("  Medellín  ");

    assertEquals("Medellín", city.getValue());
  }

  @Test
  @DisplayName("Equals should return true for equal values")
  void equalsShouldReturnTrue() {
    City c1 = new City("Bogotá");
    City c2 = new City("Bogotá");

    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());
  }

  @Test
  @DisplayName("Equals should return false for different values")
  void equalsShouldReturnFalse() {
    City c1 = new City("Bogotá");
    City c2 = new City("Cali");

    assertNotEquals(c1, c2);
  }

  @Test
  @DisplayName("toString should return the city value")
  void toStringShouldReturnValue() {
    City city = new City("Medellín");

    assertEquals("Medellín", city.toString());
  }
}