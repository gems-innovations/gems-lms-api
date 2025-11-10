package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;

import java.time.LocalDate;
import java.util.Objects;

public class BirthDate {
    private final LocalDate value;

    public BirthDate(LocalDate value) {
        if (value == null) {
            throw new IllegalArgumentException(StudentsConstants.BIRTHDATE_CANNOT_BE_NULL);
        }
        if (value.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(StudentsConstants.BIRTHDATE_CANNOT_BE_IN_THE_FUTURE);
        }
        this.value = value;
    }

    public LocalDate getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BirthDate that = (BirthDate) o;
        return Objects.equals(value, that.value);
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
