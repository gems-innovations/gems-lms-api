package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;

import java.util.Objects;

public class Name {
    private final String value;

    public Name(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(StudentsConstants.STUDENT_NAME_CANNOT_BE_NULL_OR_EMPTY);
        }
        if (value.length() < StudentsConstants.NAME_MIN_LENGTH_VALUE) {
            throw new IllegalArgumentException(StudentsConstants.NAME_MIN_LENGTH);
        }
        if (value.length() > StudentsConstants.NAME_MAX_LENGTH_VALUE) {
            throw new IllegalArgumentException(StudentsConstants.NAME_MAX_LENGTH);
        }
        this.value = value.trim();
    }

    public String getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Name)) return false;
        Name that = (Name) o;
        return Objects.equals(value, that.value);
    }

    @Override public int hashCode() { return Objects.hash(value); }

    @Override public String toString() { return value; }
}
