package com.gems.auth.application;

import com.gems.auth.application.exceptions.PasswordChangeException;
import com.gems.auth.application.gateway.PasswordEncoderGateway;
import com.gems.auth.application.gateway.PasswordResetGateway;
import com.gems.auth.application.gateway.PasswordResetNotifier;
import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * "Forgot password": a one-time link valid for {@link #TOKEN_TTL}. Requesting a link never
 * reveals whether the e-mail has an account.
 */
public class PasswordRecoveryUseCase {
  static final Duration TOKEN_TTL = Duration.ofHours(1);
  private static final SecureRandom RANDOM = new SecureRandom();

  private final UserGateway userGateway;
  private final PasswordResetGateway resetGateway;
  private final PasswordResetNotifier notifier;
  private final PasswordEncoderGateway passwordEncoderGateway;

  public PasswordRecoveryUseCase(UserGateway userGateway, PasswordResetGateway resetGateway,
                                 PasswordResetNotifier notifier, PasswordEncoderGateway passwordEncoderGateway) {
    this.userGateway = userGateway;
    this.resetGateway = resetGateway;
    this.notifier = notifier;
    this.passwordEncoderGateway = passwordEncoderGateway;
  }

  /** Sends a reset link to an active account with this e-mail; does nothing otherwise. */
  public Mono<Void> requestReset(String email) {
    Email address;
    try {
      address = new Email(email);
    } catch (IllegalArgumentException e) {
      return Mono.empty();
    }
    return userGateway.findByEmail(address)
      .filter(user -> Boolean.TRUE.equals(user.isActive()))
      .flatMap(user -> {
        String token = newToken();
        return resetGateway.save(user.getId().getValue(), hash(token), LocalDateTime.now().plus(TOKEN_TTL))
          .then(Mono.defer(() -> notifier.sendResetLink(user.getEmail().getValue(), user.getFirstName().getValue(), token)));
      });
  }

  /** Sets a new password with a valid token; the token cannot be used again. */
  public Mono<Void> reset(String token, String newPassword) {
    return Mono.defer(() -> {
      String validated = new Password(newPassword).getValue();
      String tokenHash = hash(token == null ? "" : token);
      LocalDateTime now = LocalDateTime.now();
      return resetGateway.findValidUser(tokenHash, now)
        .switchIfEmpty(Mono.error(invalidToken()))
        .flatMap(userId -> resetGateway.markUsed(tokenHash, now)
          .flatMap(marked -> Boolean.TRUE.equals(marked)
            ? userGateway.updatePassword(new UserId(userId), passwordEncoderGateway.encode(validated))
            : Mono.error(invalidToken())));
    });
  }

  private static PasswordChangeException invalidToken() {
    return new PasswordChangeException(PasswordChangeException.INVALID_RESET_TOKEN,
      "The reset link is invalid or has expired");
  }

  private static String newToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String hash(String token) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
