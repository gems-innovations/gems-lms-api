package com.gems.auth.domain.values;

import com.gems.auth.domain.constants.AuthDomainConstants;

import java.util.Objects;
import java.util.regex.Pattern;

public class Password {
  private static final Pattern PASSWORD_PATTERN = Pattern.compile(
    AuthDomainConstants.PASSWORD_PATTERN_REGEX
  );

  private final String value;

  public Password(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(AuthDomainConstants.PASSWORD_CANNOT_BE_NULL_OR_EMPTY);
    }
    if (value.length() < AuthDomainConstants.PASSWORD_MIN_LENGTH_VALUE) {
      throw new IllegalArgumentException(AuthDomainConstants.PASSWORD_MIN_LENGTH);
    }
    if (!PASSWORD_PATTERN.matcher(value).matches()) {
      throw new IllegalArgumentException(AuthDomainConstants.PASSWORD_PATTERN);
    }
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Password password = (Password) o;
    return Objects.equals(value, password.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
