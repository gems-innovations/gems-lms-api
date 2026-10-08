package com.gems.admin.infrastructure.constants;

public class AdminInfraConstants {
  public static final String BRANDING_NOT_FOUND_CODE = "BRANDING_NOT_FOUND";
  public static final String INSTITUTION_NOT_FOUND_CODE = "INSTITUTION_NOT_FOUND";
  public static final String INSTITUTION_ALREADY_EXISTS_CODE = "INSTITUTION_ALREADY_EXISTS";
  public static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
  public static final String INTERNAL_SERVER_ERROR_CODE = "INTERNAL_SERVER_ERROR";


  public static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error occurred";
  public static final String VALIDATION_FAILED = "Validation failed";

  private AdminInfraConstants() {
    throw new UnsupportedOperationException("Utility class");
  }
}
