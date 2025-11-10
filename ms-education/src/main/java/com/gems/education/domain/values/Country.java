package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;

import java.util.Objects;

public class Country {
    private final String value;

    public Country(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(StudentsConstants.COUNTRY_CANNOT_BE_NULL_OR_EMPTY);
        }
        if (value.length() < 2) {
            throw new IllegalArgumentException(StudentsConstants.COUNTRY_MIN_LENGTH);
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
        Country country = (Country) o;
        return Objects.equals(value, country.value);
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
