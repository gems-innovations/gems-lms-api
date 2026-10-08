package com.gems.shared.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InternalApiKeyTest {

  @Test
  void usesTheJwtSecretWhenNoKeyOfItsOwnIsConfigured() {
    assertEquals("jwt-secret", new InternalApiKey("", "jwt-secret").value());
    assertEquals("jwt-secret", new InternalApiKey("   ", "jwt-secret").value());
    assertTrue(new InternalApiKey(null, "jwt-secret").matches("jwt-secret"));
  }

  @Test
  void aConfiguredKeyReplacesTheJwtSecret() {
    var key = new InternalApiKey("internal-key", "jwt-secret");

    assertTrue(key.matches("internal-key"));
    assertFalse(key.matches("jwt-secret"));
  }

  @Test
  void requireRejectsMissingAndWrongKeys() {
    var key = new InternalApiKey("internal-key", "jwt-secret");

    assertDoesNotThrow(() -> key.require("internal-key"));
    assertThrows(ForbiddenException.class, () -> key.require("other"));
    assertThrows(ForbiddenException.class, () -> key.require(""));
    assertThrows(ForbiddenException.class, () -> key.require(null));
  }
}
