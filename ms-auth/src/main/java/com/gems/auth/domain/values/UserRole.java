package com.gems.auth.domain.values;

import com.gems.auth.domain.constants.AuthDomainConstants;

public enum UserRole {
  STUDENT,
  INSTRUCTOR,
  ADMIN,
  SUPER_ADMIN,
  /** Independent Taps creator: no institution, no access to courses or administration. */
  CREATOR;

  public static UserRole fromString(String value) {
    for (UserRole role : UserRole.values()) {
      if (role.name().equalsIgnoreCase(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException(AuthDomainConstants.INVALID_USER_ROLE);
  }
}
