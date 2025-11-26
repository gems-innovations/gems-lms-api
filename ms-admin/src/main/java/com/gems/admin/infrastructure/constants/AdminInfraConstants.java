package com.gems.admin.infrastructure.constants;

public class AdminInfraConstants {
  public static final String BRANDING_NOT_FOUND_CODE = "BRANDING_NOT_FOUND";
  public static final String BRANDING_ALREADY_EXISTS_CODE = "BRANDING_ALREADY_EXISTS";
  public static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";
  public static final String INTERNAL_SERVER_ERROR_CODE = "INTERNAL_SERVER_ERROR";

  public static final String BRANDING_API_BASE_PATH = "/api/v1/branding";

  public static final String COMPANY_ID_REQUIRED_MESSAGE = "Company ID is required";
  public static final String DOMAIN_VALID_MESSAGE = "Domain must be a valid domain name";
  public static final String COLOR_PATTERN_MESSAGE = "Color must be a valid hex color (e.g., #3B82F6)";
  public static final String COLOR_PATTERN_REGEX = "^#([A-Fa-f0-9]{6})$";
  public static final String VALIDATION_FAILED = "Validation failed";

  private AdminInfraConstants() {
    throw new UnsupportedOperationException("Utility class");
  }
}
