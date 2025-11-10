package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;

import java.util.Objects;

public class City {
    private final String value;

    public City(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(StudentsConstants.CITY_CANNOT_BE_NULL_OR_EMPTY);
        }
        if (value.length() < 2) {
            throw new IllegalArgumentException(StudentsConstants.CITY_MIN_LENGTH);
        }
        this.value = value.trim();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return Objects.equals(value, city.value);
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
