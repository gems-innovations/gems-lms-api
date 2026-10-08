package com.gems.shared.security;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Single place that turns the configured {@code JWT_SECRET} into the HS512 signing key, so the
 * issuer (ms-auth) and every verifier derive exactly the same key. {@link JwtSecretGuard} refuses
 * to start with a secret under {@value JwtSecretGuard#MIN_BYTES} bytes; a short secret is never
 * silently padded.
 */
public final class JwtKeys {

  private JwtKeys() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static SecretKey signingKey(String secret) {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
}
