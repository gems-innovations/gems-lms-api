package com.gems.education.domain.values;

import com.gems.education.domain.constants.StudentsConstants;

public enum DocumentType {
    CC(StudentsConstants.CC),
    TI(StudentsConstants.TI),
    CE(StudentsConstants.CE),
    PASAPORTE(StudentsConstants.PASAPORTE),
    DNI(StudentsConstants.DNI);

    private final String description;

    DocumentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static DocumentType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(StudentsConstants.DOCUMENT_TYPE_CANNOT_BE_NULL_OR_EMPTY);
        }

        String normalized = value.trim().toUpperCase();

        for (DocumentType type : DocumentType.values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }

        throw new IllegalArgumentException(StudentsConstants.INVALID_DOCUMENT_TYPE);
    }

    @Override
    public String toString() {
        return name();
    }
}
