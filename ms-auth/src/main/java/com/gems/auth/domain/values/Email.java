package com.gems.auth.domain.values;

import com.gems.auth.domain.constants.AuthDomainConstants;

import java.util.Objects;
import java.util.regex.Pattern;

public class Email {
  private static final Pattern EMAIL_PATTERN = Pattern.compile(
    AuthDomainConstants.EMAIL_PATTERN_REGEX
  );

  private final String value;

  public Email(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(AuthDomainConstants.EMAIL_CANNOT_BE_NULL_OR_EMPTY);
    }
    String trimmedValue = value.trim().toLowerCase();
    if (!EMAIL_PATTERN.matcher(trimmedValue).matches()) {
      throw new IllegalArgumentException(AuthDomainConstants.INVALID_EMAIL_FORMAT);
    }
    this.value = trimmedValue;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Email email = (Email) o;
    return Objects.equals(value, email.value);
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
