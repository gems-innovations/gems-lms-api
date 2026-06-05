package com.gems.education.domain.constants;

public class StudentsConstants {
  private StudentsConstants() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static final String STUDENT_NOT_FOUND_MESSAGE = "Student with id %s not found";
  public static final String STUDENT_ALREADY_EXISTS_EMAIL_MESSAGE = "Student with email %s already exists";
  public static final String STUDENT_ALREADY_EXISTS_DOCUMENT_MESSAGE = "Student with document number %s already exists";
  public static final String STUDENT_ID_CANNOT_BE_NULL_OR_EMPTY = "Student ID cannot be null or empty";
  public static final String NAME_MIN_LENGTH = "Name must be at least 2 characters long";
  public static final String NAME_MAX_LENGTH = "Name cannot exceed 50 characters";
  public static final String STUDENT_NAME_CANNOT_BE_NULL_OR_EMPTY = "Name cannot be null or empty";
  public static final String EMAIL_CANNOT_BE_NULL_OR_EMPTY = "Email cannot be null or empty";
  public static final String INVALID_EMAIL_FORMAT = "Invalid email format";
  public static final String BIRTHDATE_CANNOT_BE_NULL = "Birth date cannot be null";
  public static final String BIRTHDATE_CANNOT_BE_IN_THE_FUTURE = "Birth date cannot be in the future";
  public static final String COUNTRY_CANNOT_BE_NULL_OR_EMPTY = "Country cannot be null or empty";
  public static final String COUNTRY_MIN_LENGTH = "Country name must be at least 2 characters long";
  public static final String CITY_CANNOT_BE_NULL_OR_EMPTY = "City cannot be null or empty";
  public static final String CITY_MIN_LENGTH = "City name must be at least 2 characters long";
  public static final String DOCUMENT_TYPE_CANNOT_BE_NULL_OR_EMPTY = "Document type cannot be null or empty";
  public static final String CC = "Cédula de Ciudadanía";
  public static final String TI = "Tarjeta de Identidad";
  public static final String CE = "Cédula de Extranjería";
  public static final String PASSPORT = "Pasaporte";
  public static final String DNI = "Documento Nacional de Identidad";
  public static final String INVALID_DOCUMENT_TYPE = "Invalid document type. Accepted values: CC, TI, CE, PASAPORTE, DNI";
  public static final String DOCUMENT_NUMBER_CANNOT_BE_NULL_OR_EMPTY = "Document number cannot be null or empty";
  public static final String DOCUMENT_NUMBER_MIN_LENGTH = "Document number must be at least 5 characters long";
  public static final String DOCUMENT_NUMBER_MAX_LENGTH = "Document number cannot exceed 20 characters";
  public static final String DOCUMENT_NUMBER_PATTERN = "Document number can only contain letters, numbers and dashes";
  public static final int NAME_MIN_LENGTH_VALUE = 2;
  public static final int NAME_MAX_LENGTH_VALUE = 50;
  public static final int DOCUMENT_NUMBER_MIN_LENGTH_VALUE = 5;
  public static final int DOCUMENT_NUMBER_MAX_LENGTH_VALUE = 20;
  public static final String EMAIL_PATTERN_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
  public static final String DOCUMENT_NUMBER_PATTERN_REGEX = "^[a-zA-Z0-9-]+$";
}
