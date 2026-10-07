package com.gems.auth.application.gateway;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/** E-mail verification state and one-time tokens. Only a hash of each token is stored. */
public interface EmailVerificationGateway {
  /** Stores a fresh token for the user, replacing the previous one; does nothing if already verified. */
  Mono<Void> issue(Long userId, String tokenHash, LocalDateTime expiresAt, LocalDateTime now);

  /** Marks the e-mail verified and emits the user; empty if the token is unknown, used or expired. */
  Mono<Long> confirm(String tokenHash, LocalDateTime now);

  Mono<Boolean> isVerified(Long userId);

  /** When the last link was sent; empty if none was. */
  Mono<LocalDateTime> lastSentAt(Long userId);
}
