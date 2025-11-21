package com.gems.auth.infrastructure.driven.encoder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderAdapterTest {

  private PasswordEncoderAdapter passwordEncoderAdapter;

  @BeforeEach
  void setUp() {
    passwordEncoderAdapter = new PasswordEncoderAdapter();
  }

  @Test
  void shouldEncodePassword() {
    // Given
    String rawPassword = "myPassword123";

    // When
    String encodedPassword = passwordEncoderAdapter.encode(rawPassword);

    // Then
    assertNotNull(encodedPassword);
    assertNotEquals(rawPassword, encodedPassword);
    assertTrue(encodedPassword.startsWith("$2a$")); // BCrypt prefix
  }

  @Test
  void shouldEncodePasswordDifferentlyEachTime() {
    // Given
    String rawPassword = "myPassword123";

    // When
    String encodedPassword1 = passwordEncoderAdapter.encode(rawPassword);
    String encodedPassword2 = passwordEncoderAdapter.encode(rawPassword);

    // Then
    assertNotEquals(encodedPassword1, encodedPassword2);
  }

  @Test
  void shouldMatchCorrectPassword() {
    // Given
    String rawPassword = "myPassword123";
    String encodedPassword = passwordEncoderAdapter.encode(rawPassword);

    // When
    Boolean matches = passwordEncoderAdapter.matches(rawPassword, encodedPassword);

    // Then
    assertTrue(matches);
  }

  @Test
  void shouldNotMatchIncorrectPassword() {
    // Given
    String rawPassword = "myPassword123";
    String wrongPassword = "wrongPassword456";
    String encodedPassword = passwordEncoderAdapter.encode(rawPassword);

    // When
    Boolean matches = passwordEncoderAdapter.matches(wrongPassword, encodedPassword);

    // Then
    assertFalse(matches);
  }

  @Test
  void shouldReturnFalseWhenEncodedPasswordIsInvalid() {
    // Given
    String rawPassword = "myPassword123";
    String invalidEncodedPassword = "notAValidBCryptHash";

    // When
    Boolean matches = passwordEncoderAdapter.matches(rawPassword, invalidEncodedPassword);

    // Then
    assertFalse(matches);
  }
}