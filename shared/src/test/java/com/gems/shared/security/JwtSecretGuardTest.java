package com.gems.shared.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretGuardTest {

  @Test
  void rejectsMissingShortOrKnownSecrets() {
    assertThrows(IllegalStateException.class, () -> JwtSecretGuard.check(null));
    assertThrows(IllegalStateException.class, () -> JwtSecretGuard.check(" "));
    assertThrows(IllegalStateException.class, () -> JwtSecretGuard.check("mySecretKey123456789012345678901234567890"));
    assertThrows(IllegalStateException.class, () -> JwtSecretGuard.check("x".repeat(63)));
  }

  @Test
  void acceptsA64ByteSecret() {
    assertDoesNotThrow(() -> JwtSecretGuard.check("k".repeat(64)));
  }
}
