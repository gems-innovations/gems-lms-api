package com.gems.auth.domain.values;

import com.gems.auth.domain.constants.AuthDomainConstants;

public enum UserRole {
  STUDENT,
  TEACHER,
  ADMIN,
  SUPER_ADMIN;

  public static UserRole fromString(String value) {
    for (UserRole role : UserRole.values()) {
      if (role.name().equalsIgnoreCase(value)) {
        return role;
      }
    }
    throw new IllegalArgumentException(AuthDomainConstants.INVALID_USER_ROLE);
  }
}
