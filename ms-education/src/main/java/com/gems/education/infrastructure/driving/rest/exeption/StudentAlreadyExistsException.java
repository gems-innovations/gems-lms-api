package com.gems.education.infrastructure.driving.rest.exeption;

import com.gems.education.domain.constants.StudentsConstants;
public class StudentAlreadyExistsException extends RuntimeException {

    public StudentAlreadyExistsException(String message) {
        super(message);
    }

    public StudentAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public static StudentAlreadyExistsException withEmail(String email) {
        return new StudentAlreadyExistsException(
                String.format(StudentsConstants.STUDENT_ALREADY_EXISTS_MESSAGE, email)
        );
    }
}
