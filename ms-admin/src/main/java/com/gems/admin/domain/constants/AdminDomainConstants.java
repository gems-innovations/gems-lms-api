package com.gems.admin.domain.constants;

public class AdminDomainConstants {
  public static final String BRANDING_ID_CANNOT_BE_NULL = "Branding ID cannot be null";
  public static final String COMPANY_ID_CANNOT_BE_NULL_OR_EMPTY = "Company ID cannot be null or empty";
  public static final String COMPANY_ID_MIN_LENGTH = "Company ID must be at least 1 character long";
  public static final String COMPANY_ID_MAX_LENGTH = "Company ID cannot exceed 100 characters";

  public static final int COMPANY_ID_MIN_LENGTH_VALUE = 1;
  public static final int COMPANY_ID_MAX_LENGTH_VALUE = 100;

  private AdminDomainConstants() {
    throw new UnsupportedOperationException("Utility class");
  }
}
