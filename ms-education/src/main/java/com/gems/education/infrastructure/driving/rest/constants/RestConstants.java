package com.gems.education.infrastructure.driving.rest.constants;

public class RestConstants {

    public static final String STUDENT_ALREADY_EXISTS_CODE = "STUDENT_ALREADY_EXISTS";
    public static final String STUDENT_NOT_FOUND_CODE = "STUDENT_NOT_FOUND";
    public static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
    public static final String INTERNAL_SERVER_ERROR_CODE = "INTERNAL_SERVER_ERROR";

    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error occurred";

    public static final String STUDENTS_API_BASE_PATH = "/api/v1/students";
    public static final String REGISTER_ENDPOINT = "/register";
    public static final String FIND_BY_ID_ENDPOINT = "/{id}";
    public static final String FIND_ALL_ENDPOINT = "";
    public static final String DELETE_BY_ID_ENDPOINT = "/{id}";

    public static final String NAME_REQUIRED_MESSAGE = "Name is required";
    public static final String NAME_SIZE_MESSAGE = "Name must be between 2 and 50 characters";

    public static final String EMAIL_REQUIRED_MESSAGE = "Email is required";
    public static final String EMAIL_VALID_MESSAGE = "Email must be a valid email address";

    public static final String BIRTHDATE_REQUIRED_MESSAGE = "Birth date is required";
    public static final String BIRTHDATE_FUTURE_MESSAGE = "Birth date cannot be in the future";

    public static final String COUNTRY_REQUIRED_MESSAGE = "Country is required";
    public static final String CITY_REQUIRED_MESSAGE = "City is required";

    public static final String DOCUMENT_TYPE_REQUIRED_MESSAGE = "Document type is required";
    public static final String DOCUMENT_TYPE_INVALID_MESSAGE = "Document type must be one of: CC, TI, CE, PASAPORTE, DNI";

    public static final String DOCUMENT_NUMBER_REQUIRED_MESSAGE = "Document number is required";
    public static final String DOCUMENT_NUMBER_SIZE_MESSAGE = "Document number must be between 5 and 20 characters";
    public static final String DOCUMENT_NUMBER_PATTERN_MESSAGE = "Document number can only contain letters, numbers, and dashes";

    public static final String EMAIL_PATTERN_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    public static final String DOCUMENT_NUMBER_PATTERN_REGEX = "^[a-zA-Z0-9-]+$";

    private RestConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
