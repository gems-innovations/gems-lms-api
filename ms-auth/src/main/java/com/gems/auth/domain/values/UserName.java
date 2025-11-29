package com.gems.auth.domain.values;

import com.gems.auth.domain.constants.AuthDomainConstants;

import java.util.Objects;

public class UserName {
  private final String value;

  public UserName(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(AuthDomainConstants.USER_NAME_CANNOT_BE_NULL_OR_EMPTY);
    }
    if (value.length() < AuthDomainConstants.USER_NAME_MIN_LENGTH_VALUE) {
      throw new IllegalArgumentException(AuthDomainConstants.USER_NAME_MIN_LENGTH);
    }
    if (value.length() > AuthDomainConstants.USER_NAME_MAX_LENGTH_VALUE) {
      throw new IllegalArgumentException(AuthDomainConstants.USER_NAME_MAX_LENGTH);
    }
    this.value = value.trim();
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserName userName = (UserName) o;
    return Objects.equals(value, userName.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
