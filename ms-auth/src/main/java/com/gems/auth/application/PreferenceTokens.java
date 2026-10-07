package com.gems.auth.application;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

/**
 * Signed, stateless tokens for the link at the bottom of every e-mail: whoever holds it can change that
 * user's e-mail preferences without signing in, and nothing else. Format: {@code <userId>.<hmac>}.
 */
public class PreferenceTokens {
  private final byte[] key;

  public PreferenceTokens(String secret) {
    this.key = ("email-preferences:" + secret).getBytes(StandardCharsets.UTF_8);
  }

  public String create(Long userId) {
    return userId + "." + sign(userId);
  }

  /** The user of a valid token; empty for anything else. */
  public Optional<Long> userOf(String token) {
    if (token == null) return Optional.empty();
    int dot = token.indexOf('.');
    if (dot < 1) return Optional.empty();
    try {
      long userId = Long.parseLong(token.substring(0, dot));
      byte[] given = token.substring(dot + 1).getBytes(StandardCharsets.UTF_8);
      byte[] expected = sign(userId).getBytes(StandardCharsets.UTF_8);
      return MessageDigest.isEqual(given, expected) ? Optional.of(userId) : Optional.empty();
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private String sign(long userId) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      byte[] digest = mac.doFinal(Long.toString(userId).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HmacSHA256 not available", e);
    }
  }
}
