package com.gems.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Refuses to start with a missing, short or publicly known JWT secret. Tokens are signed with
 * HS512, which needs a key of at least 64 bytes.
 */
@Component
public class JwtSecretGuard {
  static final int MIN_BYTES = 64;
  /** The default this project shipped with: anyone could forge tokens with it. */
  private static final String OLD_PUBLIC_DEFAULT = "mySecretKey123456789012345678901234567890";

  public JwtSecretGuard(@Value("${jwt.secret:}") String secret) {
    check(secret);
  }

  static void check(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("JWT_SECRET is not set: define it in the environment (.env)");
    }
    if (OLD_PUBLIC_DEFAULT.equals(secret)) {
      throw new IllegalStateException("JWT_SECRET is the old public default; generate a new random secret");
    }
    if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_BYTES) {
      throw new IllegalStateException("JWT_SECRET must be at least " + MIN_BYTES + " bytes long for HS512");
    }
  }
}
