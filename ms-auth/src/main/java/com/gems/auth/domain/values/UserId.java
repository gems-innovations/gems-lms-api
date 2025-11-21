package com.gems.auth.domain.values;

import com.gems.auth.domain.constants.AuthDomainConstants;

import java.util.Objects;

public class UserId {
  private final Long value;

  public UserId(Long value) {
    if (value == null) {
      throw new IllegalArgumentException(AuthDomainConstants.USER_ID_CANNOT_BE_NULL_OR_EMPTY);
    }
    this.value = value;
  }

  public static UserId generate() {
    // For auto-increment, we don't need to generate IDs
    return null;
  }

  public Long getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserId userId = (UserId) o;
    return Objects.equals(value, userId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
