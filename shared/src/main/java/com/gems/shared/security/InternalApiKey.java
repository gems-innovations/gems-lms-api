package com.gems.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Shared secret of the service-to-service endpoints ({@code /internal/**}), which the gateway never
 * routes. Set {@code INTERNAL_API_KEY} to give it a value of its own; when it is not set the JWT
 * secret is used, as these endpoints always did, so existing deployments keep working.
 * Comparison is constant-time.
 */
@Component
public class InternalApiKey {
  public static final String HEADER = "X-Internal-Key";

  private final String value;
  private final byte[] bytes;

  public InternalApiKey(@Value("${internal.api-key:}") String configured, @Value("${jwt.secret}") String jwtSecret) {
    this.value = configured == null || configured.isBlank() ? jwtSecret : configured;
    this.bytes = value.getBytes(StandardCharsets.UTF_8);
  }

  /** The key to send in {@link #HEADER} when calling another service. */
  public String value() {
    return value;
  }

  public boolean matches(String presented) {
    return presented != null && MessageDigest.isEqual(bytes, presented.getBytes(StandardCharsets.UTF_8));
  }

  /** Throws a 403 unless {@code presented} is the internal key. */
  public void require(String presented) {
    if (!matches(presented)) throw new ForbiddenException("Invalid internal key");
  }
}
